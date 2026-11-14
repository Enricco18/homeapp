import { SignInButton, UserButton } from "@clerk/nextjs";
import { auth } from "@clerk/nextjs/server";
import { Button } from "@/components/ui/button";
import { Suspense } from "react";
import Link from "next/link";

const HeaderBox = ({ children }: { children?: React.ReactNode }) => {
  return (
    <header className="flex justify-between items-center px-4 h-16 border-b bg-white">
      {children}
    </header>
  );
};

const HeaderContent = async () => {
  const { userId } = await auth();

  return (
    <HeaderBox>
      <div className="flex items-center gap-6">
        <Link href="/homes" className="font-semibold text-sm">
          🏠 Home App
        </Link>
        {userId && (
          <nav className="flex items-center gap-4">
            <Link href="/homes" className="text-sm text-zinc-600 hover:text-zinc-900 transition-colors">
              Casas
            </Link>
            <Link href="/invites" className="text-sm text-zinc-600 hover:text-zinc-900 transition-colors">
              Convites
            </Link>
          </nav>
        )}
      </div>
      {userId ? (
        <UserButton />
      ) : (
        <SignInButton>
          <Button>Entrar</Button>
        </SignInButton>
      )}
    </HeaderBox>
  );
};

const Header = () => {
  return (
    <Suspense fallback={<HeaderBox />}>
      <HeaderContent />
    </Suspense>
  );
};

export default Header;
