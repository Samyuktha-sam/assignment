# Hello World API

A small Spring Boot service exposing one endpoint, `GET /hello-world?name=<name>`, which greets
names starting with A–M and rejects everything else.

| Request                    | Status | Body                         |
|----------------------------|--------|------------------------------|
| `/hello-world?name=alice`  | 200    | `{"message":"Hello Alice"}`  |
| `/hello-world?name=zoe`    | 400    | `{"error":"Invalid Input"}`  |
| `/hello-world` / `?name=`  | 400    | `{"error":"Invalid Input"}`  |

## Prerequisites

- JDK 17 or newer on `PATH` (or `JAVA_HOME` set). Tested with Temurin 21.
- No Maven install is needed: the Maven Wrapper (`mvnw` / `mvnw.cmd`) downloads Maven 3.9.16 on
  first use, so the first run needs internet access.

Stack: Java 17 (source/target level), Spring Boot 4.1.2, JUnit 5, Maven.

## Run the application

Unix / macOS:

```sh
./mvnw spring-boot:run
```

Windows (PowerShell or cmd):

```bat
mvnw.cmd spring-boot:run
```

The service listens on `http://localhost:8080`. To use another port, append
`-Dspring-boot.run.arguments=--server.port=9090`.

## Run the tests

```sh
./mvnw test        # Unix / macOS
mvnw.cmd test      # Windows
```

## Build and run the JAR

```sh
./mvnw package                                  # Unix / macOS
java -jar target/hello-world-api-1.0.0.jar
```

```bat
mvnw.cmd package
java -jar target\hello-world-api-1.0.0.jar
```

`package` also runs the tests. Add `--server.port=9090` after the JAR name to change the port.

## Sample requests

```sh
curl -i "http://localhost:8080/hello-world?name=alice"       # 200 {"message":"Hello Alice"}
curl -i "http://localhost:8080/hello-world?name=Mike"        # 200 {"message":"Hello Mike"}
curl -i "http://localhost:8080/hello-world?name=zed"         # 400 {"error":"Invalid Input"}
curl -i "http://localhost:8080/hello-world"                  # 400 {"error":"Invalid Input"}
curl -i "http://localhost:8080/hello-world?name="            # 400 {"error":"Invalid Input"}
curl -i "http://localhost:8080/hello-world?name=%20%20bob"   # 200 {"message":"Hello Bob"}
curl -i "http://localhost:8080/hello-world?name=1alice"      # 400 {"error":"Invalid Input"}
```

On Windows PowerShell, use `curl.exe` instead of `curl` (which is an alias for `Invoke-WebRequest`).

## Assumptions

The assignment defines the A–M, N–Z and missing/empty cases. The rest are my choices for
behaviour it leaves unspecified:

1. **Whitespace:** surrounding whitespace is stripped before validation and formatting
   (`"  alice "` → `Hello Alice`). Whitespace-only names count as empty → 400.
2. **First character must be an ASCII English letter.** Names starting with a digit,
   punctuation, an emoji or a non-English letter (`é`, Cyrillic `а`, …) → 400, with the same
   `{"error":"Invalid Input"}` body.
3. **Only the first character is validated.** The rest of the name isn't checked, so
   `alice-2!` → `Hello Alice-2!`, and `alice,zed` stays one value → `Hello Alice,zed`.
4. **Formatting:** the first letter is upper-cased and the rest is kept as supplied:
   `alice` → `Alice`, `aLiCe` → `ALiCe`, `ALICE` → `ALICE`.
5. **Repeated parameter:** if `name` appears more than once, only the first value is used
   (`?name=alice&name=zed` → `Hello Alice`; `?name=zed&name=alice` → 400).
6. **Locale independence:** letter checks use explicit ASCII ranges, and upper-casing a single
   `char` does not depend on the JVM's default locale (e.g. Turkish `i` still becomes `I`).
7. Every other path and method uses Spring Boot's default behaviour (e.g. 404, 405); only
   `GET /hello-world` is part of the API.

## Architecture

```
src/main/java/com/example/helloworld/
├── HelloWorldApplication.java   Spring Boot entry point
├── HelloWorldController.java    HTTP layer: reads `name`, maps result to 200 / 400
├── GreetingService.java         Validation and formatting rules (no Spring/HTTP types)
├── GreetingResponse.java        record → {"message": ...}
└── ErrorResponse.java           record → {"error": ...}
```

A request reaches `HelloWorldController`, which reads the first `name` value from the query
parameters and calls `GreetingService.greet(name)`. The service returns `Optional<String>`:
a greeting when the name is valid, empty otherwise. The controller turns that into a
`200 GreetingResponse` or a `400 ErrorResponse`, and Spring's Jackson integration serializes the
record to JSON.

Tests:

- `GreetingServiceTest`: plain JUnit tests for the rules (boundaries, whitespace, non-letters,
  formatting, locale).
- `HelloWorldControllerTest`: `@WebMvcTest` + MockMvc tests that use the real service. Each one
  checks the status, the JSON content type and the exact body (strict comparison, so extra
  fields fail the test).
