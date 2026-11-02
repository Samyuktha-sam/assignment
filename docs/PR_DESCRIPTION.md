# Hello World API: `GET /hello-world`

## Summary

A Spring Boot 4.1 (Java 17) web service with one endpoint, `GET /hello-world?name=<name>`:

| Input                                   | Status | Body                          |
|-----------------------------------------|--------|-------------------------------|
| First letter A–M / a–m (`name=alice`)   | 200    | `{"message":"Hello Alice"}`   |
| First letter N–Z / n–z (`name=zoe`)     | 400    | `{"error":"Invalid Input"}`   |
| `name` missing, empty or whitespace-only | 400    | `{"error":"Invalid Input"}`   |

The PR includes:

- Maven Wrapper scripts for Windows and Unix, so no Maven install is needed
- 67 unit and HTTP tests
- a README with run, test and build instructions, sample requests and the assumptions I made

## Solution rationale and choices

- **Spring Boot + Maven.** Both are suggested in the brief. Spring Boot gives me the embedded
  server, JSON serialization and MockMvc testing with very little code. I used 4.1.2, the current
  stable release, which officially supports Java 17 and newer. Dependency versions come from the
  Spring Boot parent POM, so none are hand-pinned.
- **Controller and service are separate.** `GreetingService` holds all the validation and
  formatting rules and has no HTTP code in it, so it can be unit tested with a plain
  `new GreetingService()`. The controller only turns the result into a status code and a body.
  The service is passed in through the controller's constructor.
- **Invalid input returns an empty `Optional` instead of throwing.** Invalid input is an expected
  outcome here, not an exceptional one. Returning `Optional<String>` keeps the whole decision in
  one short method, and avoids a custom exception plus a global error handler for a single error
  case.
- **The controller reads the raw parameter map instead of `@RequestParam String name`.** With a
  plain `String` parameter:
  - a missing `name` makes Spring return its own 400 body (`timestamp`, `path`, …) instead of
    `{"error":"Invalid Input"}`;
  - a repeated `name` gets joined with commas (`"alice,zed"`).

  Reading the map and taking `getFirst("name")` sends every case through the same rules, and
  makes "first value wins" explicit.
- **Java records for the response bodies.** `GreetingResponse(message)` and
  `ErrorResponse(error)` are immutable and serialize to exactly one JSON field each, so the
  response shape is set by the type.
- **Letters are checked with plain ASCII ranges** (`'a'`–`'m'`, `'A'`–`'M'`), not
  `Character.isLetter`. The brief says "English alphabet", and `isLetter` would also accept `é`
  or Cyrillic `а`. Upper-casing a single ASCII character doesn't depend on the server's locale.
- **Left out on purpose:** global exception handling, Swagger/OpenAPI, Docker, Lombok, a database
  and security. None of them is needed for one endpoint, and the brief asks not to over-engineer.

### Assumptions (for behaviour the brief doesn't specify)

1. Surrounding whitespace is stripped before validation. A whitespace-only name counts as empty
   (400).
2. The first character must be an English letter. A leading digit, punctuation, emoji or
   non-English letter returns 400 with the same error body.
3. Only the first character is validated, and the rest of the name is kept as typed:
   `alice-2!` → `Hello Alice-2!`.
4. Only the first letter is upper-cased: `alice` → `Alice`, `aLiCe` → `ALiCe`, `ALICE` → `ALICE`.
5. If `name` is repeated, the first value is used: `?name=alice&name=zed` → `Hello Alice`.

## Validation performed

- `mvnw.cmd clean package` and `./mvnw verify` on JDK 21, with the build targeting Java 17:
  **67 tests, 0 failures, BUILD SUCCESS**.
- Ran the packaged JAR and sent real requests with curl. Every response had content type
  `application/json`.

| Request query                 | Response                              |
|-------------------------------|---------------------------------------|
| `?name=alice`                 | 200 `{"message":"Hello Alice"}`       |
| `?name=Mike`                  | 200 `{"message":"Hello Mike"}`        |
| `?name=zed`, `?name=Nancy`    | 400 `{"error":"Invalid Input"}`       |
| *(no query)*                  | 400 `{"error":"Invalid Input"}`       |
| `?name=`, `?name=%20%20`      | 400 `{"error":"Invalid Input"}`       |
| `?name=%20%20bob%20`          | 200 `{"message":"Hello Bob"}`         |
| `?name=1alice`, emoji, `émilie` | 400 `{"error":"Invalid Input"}`     |
| `?name=aLiCe`                 | 200 `{"message":"Hello ALiCe"}`       |
| `?name=alice&name=zed`        | 200 `{"message":"Hello Alice"}`       |
| `?name=alice,zed`             | 200 `{"message":"Hello Alice,zed"}`   |

## Code walkthrough (about 5 minutes)

### 1. Project layout

```
src/main/java/com/example/helloworld/
├── HelloWorldApplication.java   Spring Boot entry point
├── HelloWorldController.java    HTTP layer
├── GreetingService.java         validation + formatting rules
├── GreetingResponse.java        record -> {"message": ...}
└── ErrorResponse.java           record -> {"error": ...}
src/test/java/com/example/helloworld/
├── GreetingServiceTest.java     unit tests for the rules
└── HelloWorldControllerTest.java HTTP tests with MockMvc
```

The project has five small production classes. Each one has a single job, and a request passes
through them in the order listed.

### 2. Request flow

`HelloWorldApplication` is the standard `@SpringBootApplication` main class. It starts embedded
Tomcat on port 8080.

`HelloWorldController` has one handler method:

```java
@GetMapping("/hello-world")
public ResponseEntity<?> helloWorld(@RequestParam MultiValueMap<String, String> params) {
    Optional<String> greeting = greetingService.greet(params.getFirst("name"));
    if (greeting.isEmpty()) {
        return ResponseEntity.badRequest().body(ErrorResponse.INVALID_INPUT);
    }
    return ResponseEntity.ok(new GreetingResponse(greeting.get()));
}
```

1. The controller reads the query parameters as a map and takes the first `name` value. That
   value is `null` if the parameter is missing.
2. It asks `GreetingService` for a greeting.
3. If the result is empty, it returns 400 with the shared `ErrorResponse.INVALID_INPUT`.
   Otherwise it returns 200 with a `GreetingResponse`.
4. Spring's Jackson integration turns the record into JSON. `GreetingResponse("Hello Alice")`
   becomes `{"message":"Hello Alice"}`.

The controller makes no validation decisions itself; it only maps the result to HTTP.

### 3. Validation and formatting: `GreetingService`

```java
public Optional<String> greet(String name) {
    if (name == null) {
        return Optional.empty();
    }
    String stripped = name.strip();
    if (stripped.isEmpty() || !isFirstHalfLetter(stripped.charAt(0))) {
        return Optional.empty();
    }
    return Optional.of("Hello " + capitalizeFirst(stripped));
}
```

The method checks the name in this order:

1. **Missing:** `null` → invalid.
2. **Whitespace:** `strip()` removes surrounding whitespace, including Unicode spaces that
   `trim()` misses. An empty result covers both an empty and a whitespace-only name.
3. **First letter:** `isFirstHalfLetter` is a plain range check,
   `(c >= 'A' && c <= 'M') || (c >= 'a' && c <= 'm')`. Digits, punctuation, emoji (which start
   with a surrogate `char`) and accented or non-Latin letters all fall outside it, and so does
   N–Z.
4. **Formatting:** `capitalizeFirst` upper-cases the first character and keeps the rest of the
   name unchanged.

Because the check only ever looks at ASCII characters, the result is the same under any server
locale. A test sets the default locale to Turkish, where `"i".toUpperCase()` would give `İ`, and
the result is still `Hello Ivy`.

### 4. Tests

**`GreetingServiceTest`** contains plain JUnit 5 parameterized tests with no Spring context. They
run in about a second and cover:

- the boundaries `a`, `A`, `m`, `M` (accepted) and `n`, `N`, `z`, `Z` (rejected)
- `null`, an empty string, spaces, tabs, newlines and a Unicode em space
- a leading digit, punctuation, emoji, `é`, Cyrillic `а` and fullwidth `ａ`
- formatting of mixed case, and digits or punctuation after a valid first letter
- the Turkish-locale case

**`HelloWorldControllerTest`** uses `@WebMvcTest` with MockMvc and imports the real
`GreetingService`, not a mock, so each test covers the whole path from request to JSON. Every
test checks three things:

- the status code
- that the content type is `application/json`
- the exact body, compared with `JsonCompareMode.STRICT`, so any extra field fails the test

It also covers cases that only exist at the HTTP level:

- no `name` parameter
- `?name` with no value
- repeated parameters in both orders
- URL-encoded whitespace
- a comma inside a single value (to confirm it isn't split)

### 5. Running it

```sh
./mvnw test                    # Windows: mvnw.cmd test
./mvnw spring-boot:run         # Windows: mvnw.cmd spring-boot:run

curl -i "http://localhost:8080/hello-world?name=alice"   # 200 {"message":"Hello Alice"}
curl -i "http://localhost:8080/hello-world?name=zed"     # 400 {"error":"Invalid Input"}
curl -i "http://localhost:8080/hello-world"              # 400 {"error":"Invalid Input"}
```

All the rules are in one small class, and each assumption has its own tests. Changing a rule,
for example rejecting non-letters later in the name, means editing one method and its tests.
