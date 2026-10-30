package com.aaryan.calculator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CalculatorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void addReturnsJsonResult() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "2").param("b", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("add"))
                .andExpect(jsonPath("$.a").value(2.0))
                .andExpect(jsonPath("$.b").value(3.0))
                .andExpect(jsonPath("$.result").value(5.0));
    }

    @Test
    void subtractReturnsJsonResult() throws Exception {
        mockMvc.perform(get("/api/v1/subtract").param("a", "2").param("b", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("subtract"))
                .andExpect(jsonPath("$.result").value(-1.0));
    }

    @Test
    void multiplyReturnsJsonResult() throws Exception {
        mockMvc.perform(get("/api/v1/multiply").param("a", "2").param("b", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("multiply"))
                .andExpect(jsonPath("$.result").value(6.0));
    }

    @Test
    void divideReturnsJsonResult() throws Exception {
        mockMvc.perform(get("/api/v1/divide").param("a", "5").param("b", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("divide"))
                .andExpect(jsonPath("$.result").value(2.5));
    }

    @Test
    void divideByZeroReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/divide").param("a", "5").param("b", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Division by zero is not allowed"));
    }

    @Test
    void missingParameterReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'b'"));
    }

    @Test
    void nonNumericParameterReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "abc").param("b", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'a' must be a number"));
    }

    @Test
    void nonFiniteParameterReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/multiply").param("a", "Infinity").param("b", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameters 'a' and 'b' must be finite numbers"));
    }

    @Test
    void nonFiniteSecondParameterReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "1").param("b", "NaN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameters 'a' and 'b' must be finite numbers"));
        mockMvc.perform(get("/api/v1/divide").param("a", "1").param("b", "Infinity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameters 'a' and 'b' must be finite numbers"));
    }

    @Test
    void overflowingResultReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/multiply").param("a", "1e308").param("b", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Result is out of range"));
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
