package com.sensorbite.evacroute;

import com.sensorbite.evacroute.config.JettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the evacuation routing backend.
 * 
 * Starts an embedded Jetty server with the routing REST API.
 * Configuration is via environment variables - see README.md for details.
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        log.info("Starting Evacuation Routing Service...");
        log.info("Java version: {}", System.getProperty("java.version"));

        try {
            JettyServer server = new JettyServer();

            // Graceful shutdown on SIGINT/SIGTERM
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down...");
                try {
                    server.stop();
                } catch (Exception e) {
                    log.error("Error during shutdown", e);
                }
            }));

            server.start();
            server.join();

        } catch (Exception e) {
            log.error("Failed to start server", e);
            System.exit(1);
        }
    }
}
