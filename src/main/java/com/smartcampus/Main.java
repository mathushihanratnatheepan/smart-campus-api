package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import java.io.IOException;
import java.net.URI;

public class Main {
    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static HttpServer startServer() {
        ResourceConfig config = ResourceConfig.forApplicationClass(SmartCampusApplication.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = startServer();
        System.out.println("=================================================");
        System.out.println("  Smart Campus API started successfully.");
        System.out.println("  Base URL : http://localhost:8080/api/v1");
        System.out.println("  Stop     : press ENTER");
        System.out.println("=================================================");
        System.in.read();
        server.shutdownNow();
    }
}
