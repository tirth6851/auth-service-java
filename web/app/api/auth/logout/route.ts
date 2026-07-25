import { NextResponse } from "next/server";
import { logout } from "@/lib/auth-api";
import { destroySession, getSession } from "@/lib/session";

export async function POST() {
  const session = await getSession();

  if (session.refreshToken) {
    // Best-effort: logout is idempotent server-side, and we destroy the local cookie
    // regardless of whether the backend call succeeds.
    await logout(session.refreshToken).catch(() => undefined);
  }

  await destroySession();
  return NextResponse.json({ ok: true });
}
