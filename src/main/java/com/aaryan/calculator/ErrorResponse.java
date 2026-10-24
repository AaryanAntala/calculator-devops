package com.aaryan.calculator;

public record ErrorResponse(int status, String error, String message) {
}
