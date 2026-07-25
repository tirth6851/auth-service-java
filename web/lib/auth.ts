import { cache } from "react";
import { decodeAccessToken } from "@/lib/jwt";
import { TokenPair } from "@/lib/auth-api";
import { getSession, SessionData } from "@/lib/session";

export interface Identity {
  userId: number;
  email: string;
}

/**
 * Read-only identity check for Server Components (layouts, pages). Does NOT refresh the access
 * token or write cookies — Server Components may only read cookies, not write them. Token
 * freshness is handled transparently by proxy.ts before the request ever reaches here.
 * Wrapped in `cache()` so a layout and its page both reading identity in one request only
 * decrypt the session once.
 */
export const getIdentity = cache(async (): Promise<Identity | null> => {
  const session = await getSession();
  if (!session.accessToken || !session.refreshToken) {
    return null;
  }
  return { userId: session.userId, email: session.email };
});

/**
 * Decodes a fresh token pair (from signup/login) and persists it as the session. Only call this
 * from Route Handlers — that's where cookie writes are legal.
 */
export async function establishSession(pair: TokenPair): Promise<Identity> {
  const decoded = decodeAccessToken(pair.token);
  const session = await getSession();
  const data: SessionData = {
    accessToken: pair.token,
    refreshToken: pair.refreshToken,
    accessTokenExpiresAtMs: decoded.expiresAtMs,
    userId: decoded.userId,
    email: decoded.email,
  };
  Object.assign(session, data);
  await session.save();
  return { userId: decoded.userId, email: decoded.email };
}
