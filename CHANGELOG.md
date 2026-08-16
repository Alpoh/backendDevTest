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

### Fixed
- `.gitignore`: the IntelliJ `out/` build-output rule was unanchored and was silently excluding the `adapter/out/client` package from version control. Anchored to `/out/`.
- `SimilarIdsClient`: `bodyToFlux(String.class)` does not tokenize a JSON array for `String` targets (returns the whole array as one opaque string); switched to decoding a `List<String>` via `ParameterizedTypeReference` and flattening with `Flux.fromIterable`.

### Known gaps
- No handling yet for upstream failure modes: similar-ids 404, similar-ids empty list, product-detail 404, product-detail 500, product-detail timeout/slow. Planned one scenario at a time per `CLAUDE.md`'s build order.
- No per-call timeouts or bounded fan-out concurrency yet on the outbound adapters.
