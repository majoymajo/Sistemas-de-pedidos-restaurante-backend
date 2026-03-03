package com.restaurant.qa.stepdefinitions;

import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.AfterAll;
import net.serenitybdd.rest.SerenityRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hooks de Cucumber para configuración de pruebas.
 * Ejecuta antes y después de cada escenario.
 */
public class CucumberHooks {

    private static final Logger logger = LoggerFactory.getLogger(CucumberHooks.class);

    @BeforeAll
    public static void beforeAllScenarios() {
        logger.info("=== Iniciando suite de pruebas Serenity BDD ===");
        
        // Configurar RestAssured para logging detallado si es necesario
        SerenityRest.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Before
    public void beforeEachScenario() {
        logger.info("--- Preparando escenario ---");
        
        // Reset de cualquier estado previo de RestAssured
        SerenityRest.reset();
    }

    @After
    public void afterEachScenario() {
        logger.info("--- Finalizando escenario ---");
    }

    @AfterAll
    public static void afterAllScenarios() {
        logger.info("=== Suite de pruebas finalizada ===");
    }
}
