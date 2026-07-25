"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { loginSchema, signupSchema } from "@/lib/validation";

type Mode = "login" | "signup";

const COPY = {
  login: {
    title: "Log in",
    description: "Welcome back. Enter your credentials to continue.",
    submitLabel: "Log in",
    submitPendingLabel: "Logging in...",
    endpoint: "/api/auth/login",
    schema: loginSchema,
    switchPrompt: "Don't have an account?",
    switchHref: "/signup",
    switchLabel: "Sign up",
  },
  signup: {
    title: "Create an account",
    description: "Sign up with your email and a password (min. 8 characters).",
    submitLabel: "Sign up",
    submitPendingLabel: "Creating account...",
    endpoint: "/api/auth/signup",
    schema: signupSchema,
    switchPrompt: "Already have an account?",
    switchHref: "/login",
    switchLabel: "Log in",
  },
} as const;

function safeNextPath(nextPath: string | undefined): string {
  if (nextPath && nextPath.startsWith("/") && !nextPath.startsWith("//")) {
    return nextPath;
  }
  return "/account";
}

export function AuthForm({ mode, nextPath }: { mode: Mode; nextPath?: string }) {
  const router = useRouter();
  const copy = COPY[mode];
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    const parsed = copy.schema.safeParse({ email, password });
    if (!parsed.success) {
      setError(parsed.error.issues[0]?.message ?? "Invalid input");
      return;
    }

    setPending(true);
    try {
      const res = await fetch(copy.endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(parsed.data),
      });
      const result = await res.json().catch(() => ({ ok: false }));

      if (!res.ok || !result.ok) {
        if (res.status === 429) {
          setError("Too many login attempts. Please try again later.");
        } else {
          setError(result.error ?? "Something went wrong. Please try again.");
        }
        setPending(false);
        return;
      }

      router.push(safeNextPath(nextPath));
      router.refresh();
    } catch {
      setError("Could not reach the server. Please try again.");
      setPending(false);
    }
  }

  return (
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle>{copy.title}</CardTitle>
        <CardDescription>{copy.description}</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
          <div className="flex flex-col gap-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <Button type="submit" disabled={pending} className="w-full">
            {pending ? copy.submitPendingLabel : copy.submitLabel}
          </Button>
        </form>
        <p className="mt-4 text-center text-sm text-muted-foreground">
          {copy.switchPrompt}{" "}
          <Link href={copy.switchHref} className="underline underline-offset-4">
            {copy.switchLabel}
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
