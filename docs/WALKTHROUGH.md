# Walkthrough speaking notes (about 5 minutes)

Preparation notes for recording the code walkthrough. Times are approximate.

## 1. Requirements and assumptions (0:00–1:00)

- One endpoint: `GET /hello-world?name=...`.
- The spec defines three cases: A–M → 200 with `{"message":"Hello Alice"}`; N–Z → 400; missing or
  empty → 400. Both errors return `{"error":"Invalid Input"}`.
- What the spec doesn't say, and what I chose (open README → Assumptions):
  - Strip surrounding whitespace first; whitespace-only counts as empty.
  - The first character must be an ASCII English letter, so digits, punctuation, emoji, `é` → 400.
  - Only the first character is validated; the rest of the name is kept as typed.
  - Upper-case the first letter only: `aLiCe` → `ALiCe`.
  - Repeated `name` → the first value wins.

## 2. Request flow and code structure (1:00–2:00)

- Show the package: five small classes.
- `HelloWorldApplication`: the standard Boot entry point.
- `HelloWorldController`: one `@GetMapping`, with `GreetingService` injected through the
  constructor.
  - It calls `greet(params.getFirst("name"))`.
  - Empty Optional → `400` + `ErrorResponse.INVALID_INPUT`; otherwise → `200` + `GreetingResponse`.
- `GreetingResponse` / `ErrorResponse` are records. Jackson serializes each one to a single
  field, so the JSON shape is fixed by the type.

## 3. Validation and formatting (2:00–3:00)

- Open `GreetingService.greet`:
  1. `null` (missing parameter) → empty.
  2. `strip()` (Unicode-aware, unlike `trim()`) → empty string → empty.
  3. `isFirstHalfLetter`: plain ASCII ranges `A–M` / `a–m`. I didn't use `Character.isLetter`
     because it would accept accented and non-Latin letters.
  4. `Character.toUpperCase(firstChar)` + the rest of the string unchanged. It works on a single
     ASCII char, so the default locale has no effect (unlike `String.toUpperCase()` with a
     Turkish locale).
- Why the controller binds a `MultiValueMap`: with `@RequestParam String name`, Spring returns
  its own 400 body when `name` is missing and joins repeated values into `"alice,zed"`. The map
  sends every case through the same rules and makes "first value wins" explicit.
- Why `Optional` instead of exceptions: invalid input is an expected result, and this keeps the
  code free of a `@ControllerAdvice` it doesn't need.

## 4. Tests and boundary cases (3:00–4:15)

- `GreetingServiceTest` (plain JUnit, no Spring):
  - boundaries `a/A/m/M` accepted, `n/N/z/Z` rejected
  - null, empty and several kinds of whitespace
  - digit, punctuation, emoji, `é`, Cyrillic `а`, fullwidth `ａ`
  - mixed case and punctuation after a valid first letter
  - a Turkish default locale doesn't change the result
- `HelloWorldControllerTest` (`@WebMvcTest` + MockMvc, real service imported):
  - every case checks the status, `application/json`, and the **exact** body via
    `JsonCompareMode.STRICT`, so an extra field would fail
  - HTTP-only cases: no parameter, `?name` without a value, repeated parameters, URL-encoded
    whitespace, a comma inside a single value
- Run `./mvnw test`: 67 tests pass.

## 5. Run and demo (4:15–5:00)

```sh
./mvnw spring-boot:run          # or mvnw.cmd spring-boot:run on Windows
curl -i "localhost:8080/hello-world?name=alice"   # 200 {"message":"Hello Alice"}
curl -i "localhost:8080/hello-world?name=zed"     # 400 {"error":"Invalid Input"}
curl -i "localhost:8080/hello-world"              # 400 {"error":"Invalid Input"}
```

- Close: the code is small and the rules live in one class that's easy to test. The assumptions
  are written down, so changing one (e.g. rejecting non-letters later in the name) touches one
  method plus its tests.
