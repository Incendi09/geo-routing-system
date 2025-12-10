package com.sensorbite.evacroute.config;

import jakarta.servlet.ServletException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded Jetty server setup.
 * 
 * We're using embedded Jetty rather than a full application server because:
 * 1. Simple to package and deploy
 * 2. Fast startup
 * 3. Works great in Docker
 * 4. No external server installation needed
 */
public class JettyServer {

    private static final Logger log = LoggerFactory.getLogger(JettyServer.class);

    private static final String ENV_PORT = "SERVER_PORT";
    private static final int DEFAULT_PORT = 8080;

    private final Server server;
    private final int port;

    public JettyServer() {
        this.port = getPort();
        this.server = new Server(port);
    }

    public void start() throws Exception {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // Configure RESTEasy servlet
        ServletHolder servletHolder = new ServletHolder(new HttpServletDispatcher());
        servletHolder.setInitParameter("jakarta.ws.rs.Application",
                AppConfig.class.getName());

        // Also add CORS filter for frontend integration
        context.addFilter(CorsFilter.class, "/*", null);

        context.addServlet(servletHolder, "/*");
        server.setHandler(context);

        server.start();
        log.info("Server started on port {}", port);
        log.info("API available at: http://localhost:{}/api/evac/route", port);
    }

    public void join() throws InterruptedException {
        server.join();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public int getPort() {
        String portStr = System.getenv(ENV_PORT);
        if (portStr != null && !portStr.isBlank()) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid port value '{}', using default {}", portStr, DEFAULT_PORT);
            }
        }
        return DEFAULT_PORT;
    }
}
