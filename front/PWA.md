# PWA — Como funciona e integração com Next.js + Clerk

## O que é um PWA

Progressive Web App é uma aplicação web que usa APIs modernas do browser para se comportar como um app nativo: pode ser instalada na tela inicial, funciona offline e recebe notificações push. Não é um app da App Store — é o mesmo site, mas com capacidades extras.

Requisitos mínimos para o browser considerar instalável:
- HTTPS (ou `localhost` para desenvolvimento)
- `manifest.json` linkado no `<head>`
- Service Worker registrado e ativo

---

## manifest.json

Arquivo JSON que descreve o app para o browser. Precisa estar em `/public/manifest.json` e ser referenciado no `<head>`:

```html
<link rel="manifest" href="/manifest.json">
```

No Next.js App Router, basta adicionar ao objeto `metadata` no `layout.tsx`:

```ts
export const metadata = {
  manifest: "/manifest.json",
};
```

### Campos principais

| Campo | Descrição |
|---|---|
| `name` | Nome completo exibido na splash screen |
| `short_name` | Nome curto embaixo do ícone (máx ~12 chars) |
| `start_url` | URL que abre ao lançar o app instalado |
| `display` | `standalone` = sem barra do browser; `fullscreen` = tela cheia |
| `theme_color` | Cor da barra de status do celular |
| `background_color` | Fundo da splash screen de carregamento |
| `icons` | Array de ícones PNG. **192x192** e **512x512** são obrigatórios para o Chrome considerar instalável |

---

## Service Worker

Script JavaScript que roda em uma **thread separada** do browser, isolado do DOM. Age como um proxy entre a página e a rede — intercepta todas as requisições e decide se responde do cache ou vai à rede.

### Ciclo de vida

```
sw.js modificado ou primeira visita
        ↓
   install — roda uma vez; pré-cacheia assets
        ↓
   activate — assume controle; limpa caches velhos
        ↓
   fetch — intercepta cada requisição da página
```

**`install`** dispara toda vez que o arquivo `sw.js` muda. É o momento de pré-cachear o que precisa estar disponível offline.

**`activate`** dispara após o install, quando a nova versão assume. Usar para limpar caches de versões anteriores pelo nome (`CACHE_NAME`).

**`fetch`** dispara em cada requisição. Aqui você decide a estratégia de cache.

### Onde fica o cache

Na **Cache API** — uma storage do browser separada do `localStorage`, acessível dentro do Service Worker. Guarda pares `Request → Response` e é persistente (sobrevive a fechar o browser), mas o browser pode limpar se o dispositivo estiver com pouco espaço.

### Comparação de storages

| | Cache API | localStorage | sessionStorage | IndexedDB |
|---|---|---|---|---|
| Guarda | Requests/Responses HTTP | Strings | Strings | Qualquer objeto estruturado |
| Capacidade | ~GBs | ~5MB | ~5MB | ~GBs |
| Persiste | Sim | Sim | Só na aba | Sim |
| Acessível no SW | ✅ | ❌ | ❌ | ✅ |

### Estratégias de cache

**Cache First** — serve do cache; se não tiver, vai à rede. Ideal para assets estáticos (JS, CSS, imagens).

**Network First** — tenta a rede; se falhar, serve do cache. Ideal para conteúdo que muda com frequência.

**Stale While Revalidate** — serve do cache imediatamente e atualiza o cache em background. Equilíbrio entre velocidade e frescor.

**Network Only** — sempre vai à rede, nunca cacheia. **Obrigatório para rotas de API e autenticação.**

---

## Serwist (@serwist/next)

Biblioteca atual usada neste projeto (substitui o `next-pwa` original que foi abandonado). Integra o Workbox com Next.js App Router.

### Como funciona

1. O plugin `@serwist/next` em `next.config.ts` compila o arquivo `app/sw.ts` (TypeScript) para `public/sw.js` durante o build.
2. Injeta automaticamente o script de registro do SW na página — sem componente manual.
3. Preenche `self.__SW_MANIFEST` com a lista de todos os assets do Next.js para precaching.

### Configuração

```ts
// next.config.ts
import withSerwistInit from "@serwist/next";

const withSerwist = withSerwistInit({
  swSrc: "app/sw.ts",       // fonte TypeScript do SW
  swDest: "public/sw.js",   // saída compilada
  disable: process.env.NODE_ENV === "development", // desabilita no dev
});

export default withSerwist(nextConfig);
```

```ts
// app/sw.ts
import { defaultCache } from "@serwist/next/worker";
import { NetworkOnly, Serwist } from "serwist";

const serwist = new Serwist({
  precacheEntries: self.__SW_MANIFEST,
  skipWaiting: true,
  clientsClaim: true,
  runtimeCaching: [
    { matcher: /^\/api\/.*/i, handler: new NetworkOnly() },
    // ... mais regras
    ...defaultCache,
  ],
});

serwist.addEventListeners();
```

---

## PWA + Clerk — Problemas e Soluções

### 1. Redirect quebra no iOS modo standalone

Quando o PWA é instalado na tela inicial do iPhone, ele roda em uma **janela isolada** com armazenamento de cookies separado do Safari. Um redirect para `accounts.dev` (domínio externo do Clerk) abre o login **fora** do PWA — o cookie fica preso no Safari e o app continua deslogado.

**Solução:** usar os componentes embutidos do Clerk dentro de rotas do próprio app:

```
app/sign-in/[[...sign-in]]/page.tsx  →  <SignIn />
app/sign-up/[[...sign-up]]/page.tsx  →  <SignUp />
```

Assim todo o fluxo de autenticação acontece dentro da janela do PWA.

Configurar no `middleware.ts` / `proxy.ts`:
```ts
// Redirecionar para rota interna, não para accounts.dev
```

E nas variáveis de ambiente:
```env
NEXT_PUBLIC_CLERK_SIGN_IN_URL=/sign-in
NEXT_PUBLIC_CLERK_SIGN_UP_URL=/sign-up
```

### 2. Service Worker cacheando chamadas de autenticação

O Clerk usa um token JWT `__session` que expira a cada **60 segundos**. Se o SW cachear uma resposta de API ou uma página com esse token, o usuário pode ver dados antigos ou receber erros `401 Unauthorized` ao reabrir o app.

**Solução:** `NetworkOnly` para todas as rotas de autenticação:

```ts
runtimeCaching: [
  // Nunca cachear rotas de API
  {
    matcher: /^\/api\/.*/i,
    handler: new NetworkOnly(),
  },
  // Nunca cachear chamadas do Clerk
  {
    matcher: ({ url }) =>
      url.hostname.includes("clerk") ||
      url.hostname.includes("accounts.dev"),
    handler: new NetworkOnly(),
  },
  ...defaultCache,
],
```

### 3. Funcionamento offline com Clerk

O Clerk depende de conexão para renovar o JWT a cada 60 segundos. Sem internet:

- **Com internet**: SDK renova o token em background normalmente.
- **Offline após 60s**: `auth()` no servidor considera o token expirado e pode expulsar o usuário.

**Estratégia**: manter estado básico da UI no client-side (`localStorage` ou `IndexedDB`) e só exigir validação do Clerk quando a conexão voltar. Para apps que precisam funcionar genuinamente offline, usar `useAuth()` no client em vez de `auth()` no servidor para as partes offline.

### 4. OAuth (login com Google) no iOS

Pop-ups e redirecionamentos do Google OAuth podem abrir fora da janela standalone do PWA no iOS, dependendo da versão. Testar sempre no iPhone instalado na tela inicial antes de lançar.

---

## Checklist

| Item | O que fazer |
|---|---|
| **Telas de login** | `<SignIn />` e `<SignUp />` em rotas internas (`/sign-in`, `/sign-up`) |
| **Variáveis de ambiente** | `NEXT_PUBLIC_CLERK_SIGN_IN_URL=/sign-in` e `NEXT_PUBLIC_CLERK_SIGN_UP_URL=/sign-up` |
| **Service Worker** | `NetworkOnly` para `/api/*` e domínios do Clerk |
| **Apple icon** | `<link rel="apple-touch-icon">` no `<head>` para iOS |
| **iOS standalone** | Testar OAuth e redirecionamentos com app instalado na tela inicial |
| **Offline** | Usar `useAuth()` client-side para partes que precisam funcionar sem rede |

---

## iOS vs Android

| | iOS (Safari/WebKit) | Android (Chrome) |
|---|---|---|
| SW suporte | Desde iOS 11.3; melhorou muito no iOS 16+ | Suporte completo |
| Cookies standalone | Isolado do Safari | Compartilhado |
| OAuth pop-ups | Pode abrir fora do PWA | Funciona dentro |
| Push notifications | Desde iOS 16.4 (apenas com app instalado) | Suporte completo |
