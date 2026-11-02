package com.example.helloworld;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GreetingServiceTest {

    private final GreetingService service = new GreetingService();

    @Test
    void greetsNameFromSpecificationExample() {
        assertThat(service.greet("alice")).contains("Hello Alice");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "a, Hello A",
            "A, Hello A",
            "m, Hello M",
            "M, Hello M",
            "bob, Hello Bob",
            "lucy, Hello Lucy"
    })
    void acceptsFirstHalfOfAlphabetIncludingBoundaries(String name, String expected) {
        assertThat(service.greet(name)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"n", "N", "z", "Z", "nancy", "Zoe"})
    void rejectsSecondHalfOfAlphabetIncludingBoundaries(String name) {
        assertThat(service.greet(name)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", " \t\r\n "})
    void rejectsMissingEmptyAndBlankNames(String name) {
        assertThat(service.greet(name)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1alice",          // digit
            "-alice",          // punctuation
            "_alice",
            "@alice",
            "😀alice", // emoji (surrogate pair)
            "émilie",     // é: a letter, but not an English one
            "Álvaro",     // Á
            "аlice",      // Cyrillic а, looks like a Latin a
            "ａlice"       // fullwidth ａ
    })
    void rejectsNamesNotStartingWithEnglishLetter(String name) {
        assertThat(service.greet(name)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  alice  ", "\talice\n", " alice"}) // last: Unicode em space
    void stripsSurroundingWhitespaceBeforeValidating(String name) {
        assertThat(service.greet(name)).contains("Hello Alice");
    }

    @Test
    void validatesFirstCharacterAfterStripping() {
        assertThat(service.greet("  zed")).isEmpty();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "aLiCe, Hello ALiCe",
            "ALICE, Hello ALICE",
            "alice smith, Hello Alice smith"
    })
    void capitalizesOnlyFirstLetterAndPreservesTheRest(String name, String expected) {
        assertThat(service.greet(name)).contains(expected);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "a1|Hello A1",
            "alice!|Hello Alice!",
            "d'arcy|Hello D'arcy",
            "jean-luc|Hello Jean-luc",
            "alice,zed|Hello Alice,zed"
    })
    void validatesOnlyTheFirstCharacter(String name, String expected) {
        assertThat(service.greet(name)).contains(expected);
    }

    @Test
    void resultDoesNotDependOnDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            // Turkish locale upper-cases "i" to dotted "İ" when using String.toUpperCase().
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(service.greet("ivy")).contains("Hello Ivy");
        } finally {
            Locale.setDefault(original);
        }
    }
}
