package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

public class Main {

    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static void main(String[] args) throws IOException {
        final ResourceConfig rc = new ResourceConfig()
                .packages("com.smartcampus")        // auto-scan all resources & mappers
                .register(JacksonFeature.class);     // enable JSON serialisation

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);

        System.out.println("==============================================");
        System.out.println(" Smart Campus API running at: " + BASE_URI);
        System.out.println(" Press ENTER to stop the server.");
        System.out.println("==============================================");
        System.in.read();
        server.shutdownNow();
    }
}
