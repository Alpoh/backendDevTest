# Backend dev technical test
We want to offer a new feature to our customers showing similar products to the one they are currently seeing. To do this we agreed with our front-end applications to create a new REST API operation that will provide them the product detail of the similar products for a given one. [Here](./similarProducts.yaml) is the contract we agreed.

We already have an endpoint that provides the product Ids similar for a given one. We also have another endpoint that returns the product detail by product Id. [Here](./existingApis.yaml) is the documentation of the existing APIs.

**Create a Spring boot application that exposes the agreed REST API on port 5000.**

![Diagram](./assets/diagram.jpg "Diagram")

Note that _Test_ and _Mocks_ components are given, you must only implement _yourApp_.

## Testing and Self-evaluation
You can run the same test we will put through your application. You just need to have docker installed.

First of all, you may need to enable file sharing for the `shared` folder on your docker dashboard -> settings -> resources -> file sharing.

Then you can start the mocks and other needed infrastructure with the following command.
```
docker-compose up -d simulado influxdb grafana
```
Check that mocks are working with a sample request to [http://localhost:3001/product/1/similarids](http://localhost:3001/product/1/similarids).

To execute the test run:
```
docker-compose run --rm k6 run scripts/test.js
```
Browse [http://localhost:3000/d/Le2Ku9NMk/k6-performance-test](http://localhost:3000/d/Le2Ku9NMk/k6-performance-test) to view the results.

## Evaluation
The following topics will be considered:
- Code clarity and maintainability
- Performance
- Resilience

## devTest application

The implementation lives under `src/`, as a Spring Boot 4 / Java 25 Maven project using a lightweight hexagonal (ports & adapters) architecture:

```
domain/              use case + inbound/outbound ports + domain model
adapter/in/web/      REST controller (inbound adapter)
adapter/out/client/  WebClient-based upstream clients (outbound adapters)
config/              Spring configuration (WebClient bean, etc.)
```

See `CLAUDE.md` for the build order and conventions followed, and `CHANGELOG.md` for what's implemented so far.

### Running locally

Build and run with Maven:
```
mvn spring-boot:run
```
The app listens on port 5000, calling the upstream mocks at `http://localhost:3001` (override with the `upstream.base-url` property).

For manual browser/curl testing over HTTPS, activate the `local` profile (a gitignored self-signed `keystore.p12` is generated once and referenced from `application-local.yml`):
```
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```
Then hit `https://localhost:5000/...` (self-signed, so `curl -k` or click through the browser warning). The default profile stays plain HTTP so the k6 load test above is unaffected.
