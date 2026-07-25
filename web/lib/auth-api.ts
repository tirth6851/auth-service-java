export interface TokenPair {
  token: string;
  tokenType: string;
  refreshToken: string;
}

export class AuthApiError extends Error {
  constructor(
    public status: number,
    public error: string,
    public details?: string[],
    public retryAfterSeconds?: number,
  ) {
    super(error);
    this.name = "AuthApiError";
  }
}

function requireAuthApiUrl(): string {
  const url = process.env.AUTH_API_URL;
  if (!url) {
    throw new Error("AUTH_API_URL env var must be set");
  }
  return url;
}

async function throwFromErrorBody(res: Response): Promise<never> {
  let error = "Request failed";
  let details: string[] | undefined;
  try {
    const body = await res.json();
    if (typeof body?.error === "string") error = body.error;
    if (Array.isArray(body?.details)) details = body.details;
  } catch {
    // non-JSON error body — keep the generic message
  }
  const retryAfterHeader = res.headers.get("Retry-After");
  const retryAfterSeconds = retryAfterHeader ? Number(retryAfterHeader) : undefined;
  throw new AuthApiError(res.status, error, details, retryAfterSeconds);
}

async function postJson(path: string, body: unknown): Promise<Response> {
  return fetch(`${requireAuthApiUrl()}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    cache: "no-store",
  });
}

export async function signup(email: string, password: string): Promise<TokenPair> {
  const res = await postJson("/auth/signup", { email, password });
  if (!res.ok) return throwFromErrorBody(res);
  return res.json();
}

export async function login(email: string, password: string): Promise<TokenPair> {
  const res = await postJson("/auth/login", { email, password });
  if (!res.ok) return throwFromErrorBody(res);
  return res.json();
}

export async function refresh(refreshToken: string): Promise<TokenPair> {
  const res = await postJson("/auth/refresh", { refreshToken });
  if (!res.ok) return throwFromErrorBody(res);
  return res.json();
}

export async function logout(refreshToken: string): Promise<void> {
  const res = await postJson("/auth/logout", { refreshToken });
  // Logout is idempotent server-side (already-revoked -> 204); the BFF destroys the local
  // cookie regardless of the backend outcome, so only surface genuine failures.
  if (!res.ok && res.status !== 401) {
    return throwFromErrorBody(res);
  }
}
