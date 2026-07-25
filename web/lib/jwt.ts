export interface DecodedAccessToken {
  userId: number;
  email: string;
  expiresAtMs: number;
}

/**
 * Decodes (does NOT verify) the JWT access token's payload. Safe here because the token is
 * server-held and came directly from the trusted backend over the BFF's own request — it never
 * arrives from an untrusted client. Do not reuse this for tokens accepted from browser input.
 */
export function decodeAccessToken(token: string): DecodedAccessToken {
  const parts = token.split(".");
  if (parts.length !== 3) {
    throw new Error("Malformed access token");
  }

  const payloadJson = Buffer.from(parts[1], "base64url").toString("utf8");
  const payload = JSON.parse(payloadJson) as { sub?: string; email?: string; exp?: number };

  if (!payload.sub || !payload.email || !payload.exp) {
    throw new Error("Access token payload missing required claims");
  }

  return {
    userId: Number(payload.sub),
    email: payload.email,
    expiresAtMs: payload.exp * 1000,
  };
}
