package com.example.helloworld;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    private final GreetingService greetingService;

    public HelloWorldController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    /**
     * Binding the raw parameter map (rather than {@code @RequestParam String name}) means a
     * missing name reaches the service like any other invalid input, and a repeated
     * {@code name} uses its first value instead of Spring joining all values with commas.
     */
    @GetMapping("/hello-world")
    public ResponseEntity<?> helloWorld(@RequestParam MultiValueMap<String, String> params) {
        Optional<String> greeting = greetingService.greet(params.getFirst("name"));
        if (greeting.isEmpty()) {
            return ResponseEntity.badRequest().body(ErrorResponse.INVALID_INPUT);
        }
        return ResponseEntity.ok(new GreetingResponse(greeting.get()));
    }
}
