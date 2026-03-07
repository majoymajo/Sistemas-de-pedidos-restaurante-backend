package com.restaurant.reportservice.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para validar el comportamiento de seguridad
 * en las respuestas de error del report-service.
 *
 * <p>Cubre INT-SEC-02 del TEST_PLAN_V3: el report-service debe retornar
 * respuestas de error en formato JSON (Content-Type: application/json),
 * no en formato HTML.</p>
 *
 * <p>Resultado esperado: FAIL — El report-service no tiene un
 * {@code @ControllerAdvice} / {@code GlobalExceptionHandler}. Las excepciones
 * no manejadas (como {@code MissingServletRequestParameterException}) caen
 * al error handler por defecto de Spring Boot, que puede retornar HTML
 * en lugar de JSON. Las excepciones manejadas manualmente en
 * {@code ReportController} retornan {@code ResponseEntity.badRequest().build()}
 * con body vacío.</p>
 *
 * @see docs/week-3-review/TEST_PLAN_V3.md §2.10
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── INT-SEC-02: report-service retorna JSON no HTML en errores ───────

    /**
     * INT-SEC-02: Error por fecha inválida retorna JSON, no HTML.
     *
     * <p>Resultado esperado: FAIL — El controller hace
     * {@code ResponseEntity.badRequest().build()} que retorna 400 con body vacío.
     * No hay Content-Type: application/json en una respuesta sin body.
     * El TEST_PLAN requiere que TODOS los errores retornen ErrorResponse JSON.</p>
     */
    @Test
    @DisplayName("INT-SEC-02a: Error por fecha inválida retorna Content-Type: application/json")
    void invalidDateError_shouldReturnJsonNotHtml() throws Exception {
        mockMvc.perform(get("/reports")
                        .param("startDate", "invalid")
                        .param("endDate", "2026-02-25")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    /**
     * INT-SEC-02: Error por parámetros faltantes retorna JSON, no HTML.
     *
     * <p>Resultado esperado: FAIL — Sin {@code @ControllerAdvice},
     * {@code MissingServletRequestParameterException} es manejada por el
     * DefaultHandlerExceptionResolver de Spring, que puede retornar
     * HTML de la Whitelabel Error Page en lugar de JSON.</p>
     */
    @Test
    @DisplayName("INT-SEC-02b: Error por parámetros faltantes retorna Content-Type: application/json")
    void missingParamsError_shouldReturnJsonNotHtml() throws Exception {
        mockMvc.perform(get("/reports")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    /**
     * INT-SEC-02: Error por rango invertido retorna JSON, no HTML.
     *
     * <p>Resultado esperado: FAIL — El controller hace
     * {@code ResponseEntity.badRequest().build()} con body vacío.
     * La respuesta no tiene Content-Type: application/json.</p>
     */
    @Test
    @DisplayName("INT-SEC-02c: Error por rango invertido retorna Content-Type: application/json")
    void invertedRangeError_shouldReturnJsonNotHtml() throws Exception {
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-02-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    /**
     * INT-SEC-02: Las respuestas de error deben incluir estructura ErrorResponse,
     * no body vacío ni HTML.
     *
     * <p>Resultado esperado: FAIL — Ningún error del report-service incluye
     * un body con estructura ErrorResponse (timestamp, status, error, message).
     * El controller retorna body vacío y no existe GlobalExceptionHandler.</p>
     */
    @Test
    @DisplayName("INT-SEC-02d: Error del report-service incluye estructura ErrorResponse con campos requeridos")
    void errorResponse_shouldHaveRequiredFields() throws Exception {
        mockMvc.perform(get("/reports")
                        .param("startDate", "invalid")
                        .param("endDate", "2026-02-25")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}
