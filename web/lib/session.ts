import { getIronSession, IronSession, sealData, unsealData, SessionOptions } from "iron-session";
import { cookies } from "next/headers";
import { SESSION_COOKIE_NAME } from "@/lib/session-cookie-name";

export interface SessionData {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAtMs: number;
  userId: number;
  email: string;
}

const SEVEN_DAYS_SECONDS = 60 * 60 * 24 * 7;
// Mirrors iron-session's own convention (cookie maxAge = ttl - 60s) so a direct
// NextResponse cookie write (see proxy.ts) behaves the same as session.save().
export const SESSION_COOKIE_MAX_AGE_SECONDS = SEVEN_DAYS_SECONDS - 60;

function requireSessionSecret(): string {
  const secret = process.env.SESSION_SECRET;
  if (!secret || secret.length < 32) {
    throw new Error("SESSION_SECRET env var must be set and at least 32 characters long");
  }
  return secret;
}

const SESSION_PASSWORD = requireSessionSecret();

const sessionOptions: SessionOptions = {
  cookieName: SESSION_COOKIE_NAME,
  password: SESSION_PASSWORD,
  ttl: SEVEN_DAYS_SECONDS,
  cookieOptions: {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
  },
};

// --- Server Component / Route Handler access, via next/headers `cookies()`. ---
// `cookies()` only allows writes (session.save()/session.destroy()) from Route Handlers and
// Server Functions — never call those from a Server Component render (layouts, pages).

export async function getSession(): Promise<IronSession<SessionData>> {
  return getIronSession<SessionData>(await cookies(), sessionOptions);
}

export async function destroySession(): Promise<void> {
  const session = await getSession();
  session.destroy();
}

// --- Proxy-safe access. ---
// proxy.ts reads/writes cookies via NextRequest/NextResponse directly, not next/headers, so it
// seals/unseals with the same password+ttl instead of going through getIronSession.

export async function sealSession(data: SessionData): Promise<string> {
  return sealData(data, { password: SESSION_PASSWORD, ttl: SEVEN_DAYS_SECONDS });
}

export async function unsealSession(cookieValue: string): Promise<SessionData> {
  return unsealData<SessionData>(cookieValue, { password: SESSION_PASSWORD, ttl: SEVEN_DAYS_SECONDS });
}
