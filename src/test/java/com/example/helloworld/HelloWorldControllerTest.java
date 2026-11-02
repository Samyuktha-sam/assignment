package com.example.helloworld;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * HTTP-level tests using the real {@link GreetingService}, so each case checks the full
 * request-to-JSON path: status code, content type and exact body (no extra fields).
 */
@WebMvcTest(HelloWorldController.class)
@Import(GreetingService.class)
class HelloWorldControllerTest {

    private static final String INVALID_INPUT = """
            {"error": "Invalid Input"}""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsGreetingForNameInFirstHalfOfAlphabet() throws Exception {
        expectGreeting(get("/hello-world").param("name", "alice"), "Hello Alice");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "A", "m", "M"})
    void acceptsFirstHalfBoundaries(String name) throws Exception {
        expectGreeting(get("/hello-world").param("name", name), "Hello " + name.toUpperCase());
    }

    @ParameterizedTest
    @ValueSource(strings = {"n", "N", "z", "Z", "zoe"})
    void rejectsSecondHalfOfAlphabet(String name) throws Exception {
        expectInvalidInput(get("/hello-world").param("name", name));
    }

    @Test
    void rejectsMissingName() throws Exception {
        expectInvalidInput(get("/hello-world"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void rejectsEmptyAndBlankName(String name) throws Exception {
        expectInvalidInput(get("/hello-world").param("name", name));
    }

    @Test
    void rejectsNameParameterWithoutValue() throws Exception {
        expectInvalidInput(get("/hello-world?name"));
    }

    @Test
    void stripsSurroundingWhitespace() throws Exception {
        expectGreeting(get("/hello-world").param("name", "  alice  "), "Hello Alice");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1alice", "!alice", "😀alice", "émilie"})
    void rejectsNamesNotStartingWithEnglishLetter(String name) throws Exception {
        expectInvalidInput(get("/hello-world").param("name", name));
    }

    @Test
    void capitalizesFirstLetterAndPreservesTheRest() throws Exception {
        expectGreeting(get("/hello-world").param("name", "aLiCe"), "Hello ALiCe");
    }

    @Test
    void allowsDigitsAndPunctuationAfterValidFirstLetter() throws Exception {
        expectGreeting(get("/hello-world").param("name", "alice-2!"), "Hello Alice-2!");
    }

    @Test
    void keepsCommaInSingleValueRatherThanSplittingIt() throws Exception {
        expectGreeting(get("/hello-world").param("name", "alice,zed"), "Hello Alice,zed");
    }

    @Test
    void usesFirstValueWhenNameIsRepeated() throws Exception {
        expectGreeting(get("/hello-world").param("name", "alice", "zed"), "Hello Alice");
        expectInvalidInput(get("/hello-world").param("name", "zed", "alice"));
        expectInvalidInput(get("/hello-world").param("name", "", "alice"));
    }

    @Test
    void decodesUrlEncodedName() throws Exception {
        // URI overload: the String overload treats its argument as a template and re-encodes "%".
        expectGreeting(get(URI.create("/hello-world?name=%20alice%20")), "Hello Alice");
    }

    private void expectGreeting(MockHttpServletRequestBuilder request, String message)
            throws Exception {
        perform(request)
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\": \"" + message + "\"}", JsonCompareMode.STRICT));
    }

    private void expectInvalidInput(MockHttpServletRequestBuilder request) throws Exception {
        perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().json(INVALID_INPUT, JsonCompareMode.STRICT));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
