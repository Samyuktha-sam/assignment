package com.example.helloworld;

/** Error body: {@code {"error": "Invalid Input"}}. */
public record ErrorResponse(String error) {

    public static final ErrorResponse INVALID_INPUT = new ErrorResponse("Invalid Input");
}
