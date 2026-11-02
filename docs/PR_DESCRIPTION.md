# Hello World API: `GET /hello-world`

## Summary

A Spring Boot 4.1 (Java 17) service with one endpoint, `GET /hello-world?name=<name>`:

- The first letter is A–M / a–m → `200 {"message":"Hello <Name>"}`
- The first letter is N–Z / n–z → `400 {"error":"Invalid Input"}`
- The name is missing, empty or blank → `400 {"error":"Invalid Input"}`

Includes the Maven Wrapper (Windows + Unix), unit and HTTP tests, and a README with run and test
instructions and the assumptions I made.

## Solution rationale and tradeoffs

- **Spring Boot + Maven:** both are listed as options in the brief. Boot provides JSON
  serialization, an embedded server and MockMvc testing with almost no code. 4.1.2 is the current
  stable release; its documented minimum is Java 17. Dependency versions come from the Boot
  parent POM.
- **Controller / service split:** `GreetingService` holds all the rules and has no HTTP or Spring
  types in its logic, so it can be unit tested with plain `new`. The controller only maps the
  result to a status code and body. Constructor injection, no field injection.
- **`Optional<String>` instead of exceptions:** an invalid name is an expected outcome, not an
  exceptional one. Returning `Optional` keeps the flow in one method and avoids an exception
  class plus a `@ControllerAdvice` for a single error case.
- **Binding `MultiValueMap` instead of `@RequestParam String name`:** with a plain `String`, a
  missing name triggers Spring's own 400 error body, and a repeated `name` is joined with commas
  (`"alice,zed"`). Reading the map and taking `getFirst("name")` sends every case through the same
  validation and makes "first value wins" explicit.
- **Records for the response bodies:** they are immutable and serialize to exactly one field
  each. `ErrorResponse.INVALID_INPUT` is a shared constant.
- **ASCII range checks** (`c >= 'a' && c <= 'm'`) instead of `Character.isLetter`, because the
  spec means the English alphabet. `Character.isLetter` would accept `é` or Cyrillic letters.
- **Not added, on purpose:** global exception handling, custom error pages, OpenAPI, Docker,
  Lombok, persistence and security. They are out of scope for one endpoint and the brief warns
  against over-engineering.

Assumptions for unspecified behaviour (whitespace stripping, first character must be an ASCII
letter, only the first character is validated, first value wins for repeated params, formatting
of mixed case) are listed in the README.

## Validation performed

- `./mvnw verify` on Temurin JDK 21 (compiling with `--release 17`): **67 tests, 0 failures**
  (42 in `GreetingServiceTest`, 25 in `HelloWorldControllerTest`).
- Packaged the JAR, ran it with `java -jar`, and used curl to check (all responses
  `application/json`):

  | Query                    | Result                              |
  |--------------------------|-------------------------------------|
  | `?name=alice`            | 200 `{"message":"Hello Alice"}`     |
  | `?name=Mike`             | 200 `{"message":"Hello Mike"}`      |
  | `?name=zed`, `?name=Nancy` | 400 `{"error":"Invalid Input"}`  |
  | *(no query)*             | 400 `{"error":"Invalid Input"}`     |
  | `?name=`, `?name=%20%20` | 400 `{"error":"Invalid Input"}`     |
  | `?name=%20%20bob%20`     | 200 `{"message":"Hello Bob"}`       |
  | `?name=1alice`, emoji, `é` | 400 `{"error":"Invalid Input"}`   |
  | `?name=aLiCe`            | 200 `{"message":"Hello ALiCe"}`     |
  | `?name=alice&name=zed`   | 200 `{"message":"Hello Alice"}`     |
  | `?name=alice,zed`        | 200 `{"message":"Hello Alice,zed"}` |

## Five-minute walkthrough outline

1. **Requirements and assumptions** (~1 min): the three specified cases, then the gaps I filled
   (README "Assumptions").
2. **Request flow** (~1 min): `HelloWorldController` → `GreetingService.greet` →
   `GreetingResponse` / `ErrorResponse` → JSON via Jackson.
3. **Validation and formatting** (~1 min): `strip()`, ASCII range check on the first character,
   upper-case the first letter, and why `MultiValueMap` is used for the parameter.
4. **Tests** (~1.5 min): service unit tests for boundaries A/M/N/Z, whitespace, non-letters and
   locale. MockMvc tests use the real service and strict JSON comparison.
5. **Demo** (~0.5 min): `./mvnw spring-boot:run` and a few curl requests.

Detailed speaking notes: [`docs/WALKTHROUGH.md`](WALKTHROUGH.md).

<!-- Add the walkthrough recording link here once recorded. -->
