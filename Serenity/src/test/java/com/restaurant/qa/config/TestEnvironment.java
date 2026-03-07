package com.restaurant.qa.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Configuracion centralizada del entorno de pruebas.
 * Lee valores desde serenity.conf basado en el perfil activo.
 * 
 * Uso: gradle test -Denvironment=staging
 */
public class TestEnvironment {

    private static final Config config;
    private static final String activeEnvironment;

    static {
        // Lee el environment activo desde system property o usa "default"
        activeEnvironment = System.getProperty("environment", "default");
        
        // Carga serenity.conf
        Config baseConfig = ConfigFactory.load("serenity");
        
        // Obtiene la configuracion del environment activo
        if (baseConfig.hasPath("environments." + activeEnvironment)) {
            config = baseConfig.getConfig("environments." + activeEnvironment)
                    .withFallback(baseConfig);
        } else {
            config = baseConfig;
        }
    }

    /**
     * Obtiene la URL base de la API REST desde serenity.conf.
     * @return URL base configurada (ej: http://localhost:8080)
     */
    public static String getBaseUrl() {
        return getProperty("restapi.baseurl", "http://localhost:8080");
    }

    /**
     * Obtiene la URL del servicio de ordenes.
     * @return URL del order-service
     */
    public static String getOrderServiceUrl() {
        return getProperty("order.service.baseurl", getBaseUrl());
    }

    /**
     * Obtiene la URL del servicio de reportes.
     * @return URL del report-service
     */
    public static String getReportServiceUrl() {
        return getProperty("report.service.baseurl", "http://localhost:8082");
    }

    /**
     * Obtiene el token de autenticacion para el kitchen.
     * @return Token X-Kitchen-Token
     */
    public static String getKitchenToken() {
        return getProperty("kitchen.api.token", "");
    }

    /**
     * Obtiene el nombre del header para el token de cocina.
     * @return Nombre del header (default: X-Kitchen-Token)
     */
    public static String getKitchenTokenHeader() {
        return getProperty("kitchen.token.header", "X-Kitchen-Token");
    }

    /**
     * Obtiene el ambiente activo.
     * @return Nombre del ambiente (default, staging, production)
     */
    public static String getActiveEnvironment() {
        return activeEnvironment;
    }

    /**
     * Verifica si estamos en entorno de CI/CD.
     * @return true si el entorno es CI
     */
    public static boolean isCiEnvironment() {
        return "ci".equalsIgnoreCase(activeEnvironment);
    }

    private static String getProperty(String path, String defaultValue) {
        try {
            return config.hasPath(path) ? config.getString(path) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
