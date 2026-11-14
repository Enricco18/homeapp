import { defaultCache } from "@serwist/next/worker";
import { NetworkOnly, Serwist } from "serwist";
import type { PrecacheEntry, SerwistGlobalConfig } from "serwist";

declare global {
  interface WorkerGlobalScope extends SerwistGlobalConfig {
    __SW_MANIFEST: (PrecacheEntry | string)[] | undefined;
  }
}

declare const self: WorkerGlobalScope;

const serwist = new Serwist({
  precacheEntries: self.__SW_MANIFEST,
  skipWaiting: true,
  clientsClaim: true,
  navigationPreload: true,
  runtimeCaching: [
    // Nunca cachear rotas de API — respostas dependem de autenticação
    {
      matcher: /^\/api\/.*/i,
      handler: new NetworkOnly(),
    },
    // Nunca cachear chamadas de autenticação do Clerk
    {
      matcher: ({ url }) =>
        url.hostname.includes("clerk") || url.hostname.includes("accounts.dev"),
      handler: new NetworkOnly(),
    },
    // Estratégias padrão do Serwist para assets estáticos e navegação
    ...defaultCache,
  ],
});

serwist.addEventListeners();
