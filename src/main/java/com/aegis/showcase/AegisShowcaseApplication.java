package com.aegis.showcase;

import com.aegis.showcase.http.AegisHttpServer;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

public final class AegisShowcaseApplication {
    private AegisShowcaseApplication() {}

    public static void main(String[] args) throws Exception {
        Path root = Path.of(System.getProperty("aegis.root", ".")).toAbsolutePath().normalize();
        int port = resolvePort();
        AegisHttpServer server = new AegisHttpServer(root, port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "aegis-showcase-shutdown"));
        server.start();
        System.out.println("Aegis Sentinel Public Showcase running at http://localhost:" + port);
        System.out.println("Project root: " + root);
        System.out.println("Policy: public-showcase-1.0 (illustrative; private policy not included)");
        new CountDownLatch(1).await();
    }

    private static int resolvePort() {
        String configured = System.getenv().getOrDefault("PORT", "8080");
        try {
            int port = Integer.parseInt(configured);
            if (port < 1 || port > 65535) throw new NumberFormatException();
            return port;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("PORT must be an integer from 1 to 65535.");
        }
    }
}
