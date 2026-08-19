# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Maven project scaffold (`pom.xml`) on Spring Boot 4.0.0 / Java 25, with a lightweight hexagonal package layout: `domain`, `adapter/in/web`, `adapter/out/client`, `config`.
- `ProductDetail` domain record (`id`, `name`, `price`, `availability`), matching the `similarProducts.yaml` / `existingApis.yaml` contracts.
- `FindSimilarProducts` inbound port and `ProductSimilarController`, exposing `GET /product/{productId}/similar` on port 5000.
- `ObtainSimilarIds` and `FindProductDetail` outbound ports.
- `FindSimilarProductsService`: happy-path use case implementation, fanning out product-detail lookups per similar id and preserving similarity order via `flatMapSequential`.
- `SimilarIdsClient` and `ProductDetailClient`: `WebClient`-based outbound adapters calling the upstream mocks (`GET /product/{id}/similarids`, `GET /product/{id}`), each tested against WireMock.
- `WebClientConfig`: shared `WebClient` bean, base URL sourced from the `upstream.base-url` property (`http://localhost:3001`, the `simulado` mock).
- Local HTTPS support (`application-local.yml` + a gitignored self-signed `keystore.p12`) for manual browser/curl testing of `https://localhost:5000`, kept out of the default profile so the k6 load test (plain HTTP) is unaffected.
- `CLAUDE.md`: project-specific guidance for AI-assisted development, including the TDD/hexagonal build order and code-style rules (no Lombok, no MapStruct, no wildcard or inline-qualified imports, `find`/`obtain` naming convention).

- `ProductNotFoundException` (domain): raised when the upstream `similarids` lookup 404s, mapped to `404` by `ProductSimilarController`.
- `ProductDetailUnavailableException` (domain): raised by `ProductDetailClient` when an upstream product-detail call 404s, 500s, or times out (6s per-call timeout via `.timeout(...)`).
- Retry on transient upstream errors: `ProductDetailClient` retries product-detail 500s and timeouts up to twice with a 1s fixed delay (`Retry.fixedDelay`), leaving genuine 404s to fail fast (not transient).
- Bounded connection pool: `WebClientConfig` caps the shared `WebClient`'s connections at 1000 (200 expected concurrent VUs × 5 max concurrent upstream calls per request).
- Bounded fan-out concurrency: `FindSimilarProductsService` caps concurrent product-detail lookups per request at 5 via `flatMapSequential`'s concurrency parameter, instead of the previous unbounded default.
- Partial-failure degradation: `FindSimilarProductsService` now skips any similar id whose product-detail lookup fails (`onErrorResume`) and returns the rest, in order. A request where every lookup fails now completes with an empty list rather than erroring — this matches `similarProducts.yaml`'s contract, which only defines `200`/`404` responses and explicitly allows `minItems: 0`.

### Changed
- `ProductSimilarController`: removed the `@ExceptionHandler(ProductDetailUnavailableException.class)` → `502` mapping. It's unreachable now that `FindSimilarProductsService` resumes that exception internally instead of letting it propagate, and the contract never defined a `5xx` response for this endpoint in the first place.

### Fixed
- `.gitignore`: the IntelliJ `out/` build-output rule was unanchored and was silently excluding the `adapter/out/client` package from version control. Anchored to `/out/`.
- `SimilarIdsClient`: `bodyToFlux(String.class)` does not tokenize a JSON array for `String` targets (returns the whole array as one opaque string); switched to decoding a `List<String>` via `ParameterizedTypeReference` and flattening with `Flux.fromIterable`.

### Known gaps
- No circuit breaker yet: a request touching a permanently broken/slow upstream (e.g. the k6 `error`/`verySlow` scenarios, which mock unconditional failures) still pays the full timeout-plus-retry cost on every single call, rather than failing fast once the upstream is known to be down. A k6 run after the retry/concurrency changes landed showed `http_req_duration` p95 = 5.11s and max = 20.14s (up from a pre-retry max around the single 6s timeout), with 600 interrupted iterations concentrated in `verySlow`.
