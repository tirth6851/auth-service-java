# ADR-008: Trusted-Proxy Handling for Rate Limiting

**Date**: 2026-11-25  
**Status**: Proposed  
**Discussion**: issue #32

## Context

[ADR-007](007-rate-limiting-strategy.md) keys the login and signup limiters on `request.getRemoteAddr()` and deliberately ignores `X-Forwarded-For`, because any client can forge that header. The trade-off it accepted was that clients behind a shared NAT share a bucket.

A reverse proxy has the same effect on a much larger scale. The docs recommend terminating TLS at nginx, Caddy or a load balancer. Behind one, `getRemoteAddr()` is the proxy's address for every request, so all users share a single bucket: one noisy client can lock everyone out of login and signup.

Today's wording in `docs/API_CONTRACT.md` suggests `server.forward-headers-strategy=framework` for this case. Part of this ADR is to check whether that advice works and is safe.

## Decision

1. **Default does not change.** With no configuration, no proxy is trusted and the limiters key on `getRemoteAddr()`. Upgrading never weakens protection.
2. **Opt-in trusted proxies.** An operator who runs behind a proxy sets:

   ```properties
   server.forward-headers-strategy=native
   server.tomcat.remoteip.internal-proxies=<regex matching only the proxy addresses>
   ```

   `native` enables Tomcat's `RemoteIpValve`. The valve only reads `X-Forwarded-For` when the direct peer matches `internal-proxies`. It then walks the header from right to left, skips entries that match `internal-proxies`, and takes the first entry that does not match as the client address ([Tomcat `RemoteIpValve`](https://tomcat.apache.org/tomcat-10.1-doc/api/org/apache/catalina/valves/RemoteIpValve.html)). After the valve runs, `getRemoteAddr()` already returns the client address.
3. **`RateLimitInterceptor` does not change.** It keeps calling `getRemoteAddr()`. Trust is decided in one place (the servlet container), not in application code.
4. **`framework` is not recommended** for this purpose (see the claims below). The contract text is corrected in the follow-up code PR.

## Claims and how each one is proven

The code PR must prove each of these with a test that starts a real server (`@SpringBootTest(webEnvironment = RANDOM_PORT)` and a real HTTP client). `MockMvc` is not enough, because it skips the servlet container, which is where both `RemoteIpValve` and `ForwardedHeaderFilter` run. The tests send a controlled peer address and header values, and read the key the limiter used (for example by exhausting the limit and checking which sender gets 429).

| # | Claim | Proof |
|---|-------|-------|
| 1 | Default config: a forged `X-Forwarded-For` does not change the bucket. | Same peer, a different forged header on each request: the limit still triggers after N requests. |
| 2 | `native` with the peer outside `internal-proxies`: a forged header is ignored. | Peer not in the regex, forged header, limit still triggers. This is the bypass test the issue asks for. |
| 3 | `native` with the peer inside `internal-proxies`: the limiter keys on the client address from the header. | Two different client addresses behind the same trusted peer get separate buckets. |
| 4 | `native`, trusted peer, client sends its own `X-Forwarded-For: a, b` and the proxy appends the real address: the real (right-most untrusted) address is used, not the forged left-most one. | Header `forged, real` from a trusted peer: key is `real`. |
| 5 | `framework` does not make `getRemoteAddr()` return the forwarded client address. | With `framework` and a trusted-looking header, check which key the limiter used. The Spring Framework reference lists host, port, scheme and prefix for `ForwardedHeaderFilter`, and does not list `X-Forwarded-For` ([Spring Framework: Forwarded Headers](https://docs.spring.io/spring-framework/reference/web/webmvc/filters.html)). The proxy fix will not work with `framework`, and the filter does not check who sent the header. Claim 5 is documented, not yet run, and the test decides it. |

Claims 2 to 5 are written from the documentation. I could not run the Java build where I wrote this, so none of them has been run yet. If a test disagrees with the table, the ADR is updated before the code merges.

## Operator guidance

The right value of `internal-proxies` depends on what address the application sees as the direct peer. Do not guess it: log or inspect `getRemoteAddr()` once in the real deployment, then match exactly that.

### Default is too broad

Spring Boot's default for `server.tomcat.remoteip.internal-proxies` matches loopback, link-local and the private ranges `10/8`, `172.16/12` and `192.168/16` ([Spring Boot: running behind a front-end proxy](https://docs.spring.io/spring-boot/how-to/webserver.html)). With `native` and that default, any host on those ranges is trusted to set `X-Forwarded-For`, including other containers and anything that can reach the app directly. Set the property to the proxy's address (or a tight range), and never to empty in production. An empty value trusts every peer.

### docker-compose.yml

The compose file publishes the app on `8080` directly (`ports: "8080:8080"`), so with the default regex a direct client may arrive from a Docker bridge address that the default range matches. For a deployment with a proxy:

- Put the proxy in the same compose project and **do not publish** `8080` on the host. Expose it only to the proxy's network.
- Set `server.tomcat.remoteip.internal-proxies` to the proxy container's address or a narrow subnet you define for the compose network, not the whole `172.16/12` range.
- Keep `forward-headers-strategy` unset when running the compose file as shipped (direct access, no proxy).

### Render

On Render the application sits behind Render's own load balancer, and the peer address the container sees is chosen by the platform. The addresses are not something this repository controls. Before turning on `native`, record the peer address that appears in a request from the public URL, and set `internal-proxies` to that value only. If it cannot be pinned down, leave the default behaviour (shared bucket) rather than trusting a wide range. `render.yaml` gets the two properties as `sync: false` or fixed values only once that address has been confirmed.

### The proxy itself

The proxy at the edge must **overwrite or append** to `X-Forwarded-For`, never pass the client's value through unchanged, and should drop any incoming `Forwarded` and `X-Forwarded-*` headers from the outside. For nginx, `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;` appends the real peer address, which is what claim 4 relies on.

## Consequences

- No behaviour change for existing deployments.
- Operators behind a proxy get a per-client limit once they configure `internal-proxies` correctly. A wrong or too-broad value is an operator error that weakens the limit, which is why the docs name it explicitly.
- The follow-up code PR: new properties are the Spring Boot ones above (no new application properties), tests for claims 1 to 5, and updates to `docs/API_CONTRACT.md`, `docs/ENVIRONMENTS.md`, `docs/DEPLOYMENT.md` and the README security notes.
- Still true after this change: buckets are per instance and in memory (see ADR-007).

## Alternatives Rejected

| Option | Reason rejected |
|--------|----------------|
| `forward-headers-strategy=framework` | `ForwardedHeaderFilter` does not check who sent the header, and the reference does not list `X-Forwarded-For` among what it applies (claim 5). |
| Read `X-Forwarded-For` in `RateLimitInterceptor` | Duplicates trust logic in application code and is easy to get wrong. The container already implements it. |
| App-level `app.ratelimit.trusted-proxies` list | Possible, but a second place to configure what Tomcat already provides, and it would need its own right-to-left parsing and tests. |
| Always trust private ranges | The case the default regex already makes risky. |
| Leave it as is | All users behind a proxy share one bucket. |
