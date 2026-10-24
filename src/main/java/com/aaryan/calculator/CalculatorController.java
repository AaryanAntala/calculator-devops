package com.aaryan.calculator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.DoubleBinaryOperator;

@RestController
@RequestMapping("/api/v1")
public class CalculatorController {

    private final CalculatorService calculator;

    public CalculatorController(CalculatorService calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/add")
    public CalculationResponse add(@RequestParam double a, @RequestParam double b) {
        return calculate("add", a, b, calculator::add);
    }

    @GetMapping("/subtract")
    public CalculationResponse subtract(@RequestParam double a, @RequestParam double b) {
        return calculate("subtract", a, b, calculator::subtract);
    }

    @GetMapping("/multiply")
    public CalculationResponse multiply(@RequestParam double a, @RequestParam double b) {
        return calculate("multiply", a, b, calculator::multiply);
    }

    @GetMapping("/divide")
    public CalculationResponse divide(@RequestParam double a, @RequestParam double b) {
        return calculate("divide", a, b, calculator::divide);
    }

    private CalculationResponse calculate(String operation, double a, double b, DoubleBinaryOperator op) {
        // Double.parseDouble accepts "NaN" and "Infinity"; reject them as invalid input
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            throw new IllegalArgumentException("Parameters 'a' and 'b' must be finite numbers");
        }
        return new CalculationResponse(operation, a, b, op.applyAsDouble(a, b));
    }
}
