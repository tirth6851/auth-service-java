// Split out from session.ts so middleware.ts (edge runtime) can check for the cookie's
// presence without importing iron-session's config, which requires SESSION_SECRET at load time.
export const SESSION_COOKIE_NAME = "auth_portal_session";
