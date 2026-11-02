package com.example.helloworld;

import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * Validates a name and builds its greeting.
 *
 * <p>A name is valid when, after stripping surrounding whitespace, its first character is an
 * ASCII letter from the first half of the English alphabet (A–M or a–m). Only the first
 * character is checked; the rest of the name is kept as supplied.
 */
@Service
public class GreetingService {

    /**
     * Returns the greeting for {@code name}, or an empty {@code Optional} when the name is
     * missing, blank, or does not start with a letter from A–M / a–m.
     */
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

    private static boolean isFirstHalfLetter(char c) {
        return (c >= 'A' && c <= 'M') || (c >= 'a' && c <= 'm');
    }

    // Only called with an ASCII letter first, so the result does not depend on the default locale.
    private static String capitalizeFirst(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
