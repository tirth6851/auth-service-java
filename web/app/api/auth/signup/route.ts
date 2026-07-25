import { NextResponse } from "next/server";
import { signup, AuthApiError } from "@/lib/auth-api";
import { establishSession } from "@/lib/auth";
import { signupSchema } from "@/lib/validation";

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);
  const parsed = signupSchema.safeParse(body);

  if (!parsed.success) {
    return NextResponse.json(
      { ok: false, error: "Validation failed", details: parsed.error.issues.map((i) => i.message) },
      { status: 400 },
    );
  }

  try {
    const pair = await signup(parsed.data.email, parsed.data.password);
    await establishSession(pair);
    return NextResponse.json({ ok: true });
  } catch (err) {
    if (err instanceof AuthApiError) {
      return NextResponse.json(
        { ok: false, error: err.error, details: err.details },
        { status: err.status },
      );
    }
    return NextResponse.json({ ok: false, error: "Signup failed" }, { status: 502 });
  }
}
