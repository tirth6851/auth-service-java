import { NextResponse } from "next/server";
import { login, AuthApiError } from "@/lib/auth-api";
import { establishSession } from "@/lib/auth";
import { loginSchema } from "@/lib/validation";

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);
  const parsed = loginSchema.safeParse(body);

  if (!parsed.success) {
    return NextResponse.json(
      { ok: false, error: "Validation failed", details: parsed.error.issues.map((i) => i.message) },
      { status: 400 },
    );
  }

  try {
    const pair = await login(parsed.data.email, parsed.data.password);
    await establishSession(pair);
    return NextResponse.json({ ok: true });
  } catch (err) {
    if (err instanceof AuthApiError) {
      const headers = new Headers();
      if (err.retryAfterSeconds !== undefined) {
        headers.set("Retry-After", String(err.retryAfterSeconds));
      }
      return NextResponse.json({ ok: false, error: err.error }, { status: err.status, headers });
    }
    return NextResponse.json({ ok: false, error: "Login failed" }, { status: 502 });
  }
}
