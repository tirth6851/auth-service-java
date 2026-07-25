import Link from "next/link";
import { redirect } from "next/navigation";
import { getIdentity } from "@/lib/auth";
import { buttonVariants } from "@/components/ui/button";

export default async function HomePage() {
  const identity = await getIdentity();
  if (identity) {
    redirect("/account");
  }

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-6 p-6 text-center">
      <h1 className="text-3xl font-semibold tracking-tight">Auth Platform</h1>
      <p className="max-w-md text-muted-foreground">
        A minimal account portal backed by the Java auth service.
      </p>
      <div className="flex gap-4">
        <Link href="/login" className={buttonVariants({ variant: "default" })}>
          Log in
        </Link>
        <Link href="/signup" className={buttonVariants({ variant: "outline" })}>
          Sign up
        </Link>
      </div>
    </div>
  );
}
