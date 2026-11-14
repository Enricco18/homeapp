"use client";
import { useAuth, useSession } from "@clerk/nextjs";
import { useEffect, useState } from "react";

export default function TokenDebug() {
  const { getToken, userId } = useAuth();
  const { session } = useSession();
  const [sessionToken, setSessionToken] = useState<string | null>(null);

  useEffect(() => {
    if (!userId) return;
    getToken().then(setSessionToken);
  }, [userId, getToken]);

  if (!userId) return <p className="text-sm text-zinc-400">Not signed in</p>;

  return (
    <div className="w-full font-mono text-xs space-y-4">
      <div>
        <p className="font-bold text-zinc-500 mb-1">Session Token (JWT)</p>
        <pre className="bg-zinc-100 dark:bg-zinc-900 p-3 rounded break-all whitespace-pre-wrap">
          {sessionToken ?? "loading..."}
        </pre>
      </div>
      <div>
        <p className="font-bold text-zinc-500 mb-1">Client</p>
        <pre className="bg-zinc-100 dark:bg-zinc-900 p-3 rounded break-all whitespace-pre-wrap">
          {JSON.stringify(
            {
              sessionId: session?.id,
              status: session?.status,
              lastActiveAt: session?.lastActiveAt,
              expireAt: session?.expireAt,
              userId: session?.user?.id,
              email: session?.user?.primaryEmailAddress?.emailAddress,
            },
            null,
            2
          )}
        </pre>
      </div>
    </div>
  );
}
