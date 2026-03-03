package com.restaurant.qa.runners;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

/**
 * TestRunner — Punto de entrada principal para la suite de pruebas.
 *
 * Esta clase configura la integración JUnit 5 → Cucumber → Serenity.
 * JUnit 5 Platform usa esta clase como portador de configuración;
 * la clase está intencionalmente vacía.
 *
 * Ejecutar con: gradle test
 * Filtrar por tag: gradle test -Dcucumber.filter.tags="@smoke"
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("com.restaurant.qa")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.restaurant.qa.stepdefinitions")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/resources/features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "io.cucumber.core.plugin.SerenityReporter")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip")
public class TestRunner {
    // Esta clase está intencionalmente vacía.
    // La Plataforma JUnit 5 la usa como portador de configuración.
}
