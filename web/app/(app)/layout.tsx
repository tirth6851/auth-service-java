import { redirect } from "next/navigation";
import Link from "next/link";
import { getIdentity } from "@/lib/auth";
import { LogoutButton } from "@/components/logout-button";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const identity = await getIdentity();

  if (!identity) {
    redirect("/login?next=/account");
  }

  return (
    <div className="flex flex-1 flex-col">
      <header className="flex items-center justify-between border-b px-6 py-4">
        <nav className="flex gap-4 text-sm font-medium">
          <Link href="/account">Account</Link>
          <Link href="/account/security">Security</Link>
          <Link href="/account/sessions">Sessions</Link>
        </nav>
        <div className="flex items-center gap-4">
          <span className="text-sm text-muted-foreground">{identity.email}</span>
          <LogoutButton />
        </div>
      </header>
      <main className="flex flex-1 flex-col p-6">{children}</main>
    </div>
  );
}
