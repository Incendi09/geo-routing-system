package com.sensorbite.evacroute.config;

import com.sensorbite.evacroute.controller.EvacuationRouteController;
import com.sensorbite.evacroute.controller.GlobalExceptionHandler;
import com.sensorbite.evacroute.geo.FloodZoneProvider;
import com.sensorbite.evacroute.geo.MockFloodZoneProvider;
import com.sensorbite.evacroute.service.RoutingService;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application configuration.
 * 
 * This wires up our REST endpoints and services.
 * In a Spring Boot app this would be done via annotations/DI,
 * but for plain JAX-RS we do it manually here.
 * 
 * Configuration is driven by environment variables with sensible defaults
 * so the app works out of the box.
 */
@ApplicationPath("/")
public class AppConfig extends Application {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    // Environment variable names
    private static final String ENV_ROADS_PATH = "ROADS_GEOJSON_PATH";
    private static final String ENV_FLOODS_PATH = "FLOODS_GEOJSON_PATH";

    // Defaults - relative to working directory
    private static final String DEFAULT_ROADS_PATH = "data/roads.geojson";
    private static final String DEFAULT_FLOODS_PATH = "data/flood_zones.geojson";

    private final Set<Object> singletons = new HashSet<>();

    public AppConfig() {
        log.info("Initializing application...");

        // Load config from environment or use defaults
        String roadsPath = getEnvOrDefault(ENV_ROADS_PATH, DEFAULT_ROADS_PATH);
        String floodsPath = getEnvOrDefault(ENV_FLOODS_PATH, DEFAULT_FLOODS_PATH);

        log.info("Roads path: {}", roadsPath);
        log.info("Flood zones path: {}", floodsPath);

        // Create service instances
        FloodZoneProvider floodProvider = new MockFloodZoneProvider(floodsPath);
        RoutingService routingService = new RoutingService(roadsPath, floodProvider);

        // Register Jackson provider for JSON serialization
        singletons.add(new org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider());

        // Register REST resources
        singletons.add(new EvacuationRouteController(routingService));
        singletons.add(new GlobalExceptionHandler());

        log.info("Application initialized successfully");
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    private String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        if (value == null || value.isBlank()) {
            value = System.getProperty(envVar.toLowerCase().replace('_', '.'), defaultValue);
        }
        return value;
    }
}
