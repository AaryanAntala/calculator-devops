package com.aaryan.calculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorServiceTest {

    private final CalculatorService calculator = new CalculatorService();

    @Test
    void addReturnsSum() {
        assertEquals(5, calculator.add(2, 3));
        assertEquals(-1, calculator.add(2, -3));
        assertEquals(0.3, calculator.add(0.1, 0.2), 1e-9);
    }

    @Test
    void subtractReturnsDifference() {
        assertEquals(-1, calculator.subtract(2, 3));
        assertEquals(5, calculator.subtract(2, -3));
    }

    @Test
    void multiplyReturnsProduct() {
        assertEquals(6, calculator.multiply(2, 3));
        assertEquals(-6, calculator.multiply(2, -3));
        assertEquals(0, calculator.multiply(7, 0));
    }

    @Test
    void divideReturnsQuotient() {
        assertEquals(2.5, calculator.divide(5, 2));
        assertEquals(-2, calculator.divide(6, -3));
        assertEquals(0, calculator.divide(0, 5));
    }

    @Test
    void divideByZeroThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> calculator.divide(5, 0));
        assertEquals("Division by zero is not allowed", ex.getMessage());
    }
}
