import { NextRequest, NextResponse } from "next/server";
import { SESSION_COOKIE_NAME } from "@/lib/session-cookie-name";
import { SESSION_COOKIE_MAX_AGE_SECONDS, sealSession, unsealSession } from "@/lib/session";
import { refresh as refreshBackend } from "@/lib/auth-api";
import { decodeAccessToken } from "@/lib/jwt";

// Refresh when the access token is within this many ms of expiry (or already past it).
const REFRESH_SKEW_MS = 60_000;

function redirectToLogin(request: NextRequest, clearCookie = false): NextResponse {
  const loginUrl = new URL("/login", request.url);
  // Only ever redirect back to a local path — never let `next` become an open redirect.
  const nextPath = request.nextUrl.pathname + request.nextUrl.search;
  if (nextPath.startsWith("/") && !nextPath.startsWith("//")) {
    loginUrl.searchParams.set("next", nextPath);
  }
  const response = NextResponse.redirect(loginUrl);
  if (clearCookie) {
    response.cookies.delete(SESSION_COOKIE_NAME);
  }
  return response;
}

// Proxy is the only place in this app allowed to both read the session cookie AND rewrite it
// within the same request/response cycle (Server Components may only read cookies; Route
// Handlers can write them but don't run ahead of a page view). Silent refresh therefore lives
// here, not in the protected layout.
export async function proxy(request: NextRequest) {
  const cookieValue = request.cookies.get(SESSION_COOKIE_NAME)?.value;
  if (!cookieValue) {
    return redirectToLogin(request);
  }

  let session;
  try {
    session = await unsealSession(cookieValue);
  } catch {
    return redirectToLogin(request, true);
  }

  const needsRefresh = session.accessTokenExpiresAtMs - Date.now() < REFRESH_SKEW_MS;
  if (!needsRefresh) {
    return NextResponse.next();
  }

  try {
    const pair = await refreshBackend(session.refreshToken);
    const decoded = decodeAccessToken(pair.token);
    const sealed = await sealSession({
      accessToken: pair.token,
      refreshToken: pair.refreshToken,
      accessTokenExpiresAtMs: decoded.expiresAtMs,
      userId: decoded.userId,
      email: decoded.email,
    });

    const response = NextResponse.next();
    response.cookies.set(SESSION_COOKIE_NAME, sealed, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      maxAge: SESSION_COOKIE_MAX_AGE_SECONDS,
      path: "/",
    });
    return response;
  } catch {
    // Expired/revoked refresh token (or reuse-detected family revocation) — same handling either
    // way: destroy the cookie and send the user back through login.
    return redirectToLogin(request, true);
  }
}

export const config = {
  matcher: ["/account/:path*"],
};
