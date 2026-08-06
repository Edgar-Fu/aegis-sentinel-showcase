package com.aegis.showcase.http;

import com.aegis.showcase.model.AnalysisResult;
import com.aegis.showcase.service.AnalysisService;
import com.aegis.showcase.service.PublicPolicy;
import com.aegis.showcase.service.ScenarioService;
import com.aegis.showcase.util.Json;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;

public final class AegisHttpServer implements AutoCloseable {
    private static final int MAX_REQUEST_BYTES = 10 * 1024 * 1024;
    private final HttpServer server;
    private final Path projectRoot;
    private final AnalysisService analysisService;
    private final ScenarioService scenarioService;

    public AegisHttpServer(Path projectRoot, int port) throws IOException {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        this.analysisService = new AnalysisService(this.projectRoot);
        this.scenarioService = new ScenarioService(this.projectRoot, analysisService);
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", this::handle);
        this.server.setExecutor(Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors())));
    }

    public void start() {
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET,POST,OPTIONS");
                send(exchange, 204, "text/plain; charset=utf-8", new byte[0]);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/health")) {
                requireMethod(exchange, "GET");
                json(exchange, 200, Json.map(
                        "application", "Aegis Sentinel Public Showcase",
                        "status", "UP",
                        "time", Instant.now(),
                        "policyVersion", PublicPolicy.VERSION,
                        "showcaseOnly", true));
                return;
            }
            if (path.equals("/api/scenarios")) {
                requireMethod(exchange, "GET");
                json(exchange, 200, scenarioService.catalog());
                return;
            }
            if (path.startsWith("/api/scenarios/")) {
                requireMethod(exchange, "POST", "GET");
                String scenario = path.substring("/api/scenarios/".length());
                json(exchange, 200, scenarioService.run(scenario));
                return;
            }
            if (path.equals("/api/analyze")) {
                requireMethod(exchange, "POST");
                byte[] body = readLimited(exchange.getRequestBody(), MAX_REQUEST_BYTES);
                Headers headers = exchange.getRequestHeaders();
                String sourceId = header(headers, "X-Source-Id", "manual-upload");
                String fileName = header(headers, "X-Filename", "upload.bin");
                AnalysisResult result = analysisService.analyzeUpload(sourceId, fileName, body);
                json(exchange, 200, result);
                return;
            }
            if (path.startsWith("/api/evidence/") && path.endsWith("/replay")) {
                requireMethod(exchange, "POST");
                String evidenceId = path.substring("/api/evidence/".length(), path.length() - "/replay".length());
                json(exchange, 200, analysisService.evidenceService().replay(evidenceId));
                return;
            }
            if (path.startsWith("/api/evidence/") && path.endsWith("/tamper-check")) {
                requireMethod(exchange, "POST");
                String evidenceId = path.substring("/api/evidence/".length(), path.length() - "/tamper-check".length());
                json(exchange, 200, analysisService.evidenceService().tamperCheck(evidenceId));
                return;
            }
            serveStatic(exchange, path);
        } catch (IllegalArgumentException ex) {
            json(exchange, 400, Json.map("error", "BAD_REQUEST", "message", ex.getMessage()));
        } catch (MethodNotAllowedException ex) {
            exchange.getResponseHeaders().set("Allow", ex.allowed);
            json(exchange, 405, Json.map("error", "METHOD_NOT_ALLOWED", "message", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            json(exchange, 500, Json.map("error", "INTERNAL_ERROR", "message", "The public showcase could not complete the request."));
        } finally {
            exchange.close();
        }
    }

    private void serveStatic(HttpExchange exchange, String path) throws IOException {
        requireMethod(exchange, "GET");
        Path target;
        if (path.startsWith("/sample_images/")) {
            target = projectRoot.resolve(path.substring(1)).normalize();
        } else {
            String relative = path.equals("/") ? "index.html" : path.substring(1);
            target = projectRoot.resolve("web").resolve(relative).normalize();
        }
        Path allowedWeb = projectRoot.resolve("web").normalize();
        Path allowedSamples = projectRoot.resolve("sample_images").normalize();
        if (!(target.startsWith(allowedWeb) || target.startsWith(allowedSamples)) || !Files.isRegularFile(target)) {
            json(exchange, 404, Json.map("error", "NOT_FOUND", "message", "Resource not found."));
            return;
        }
        send(exchange, 200, contentType(target), Files.readAllBytes(target));
    }

    private void json(HttpExchange exchange, int status, Object body) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", (Json.pretty(body) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", contentType.startsWith("text/html") ? "no-store" : "no-cache");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private byte[] readLimited(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = in.read(buffer)) >= 0) {
            total += read;
            if (total > maxBytes) throw new IllegalArgumentException("Request exceeds the 10 MB public-demo limit.");
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private String header(Headers headers, String name, String fallback) {
        String value = headers.getFirst(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private void requireMethod(HttpExchange exchange, String... methods) {
        String actual = exchange.getRequestMethod();
        for (String method : methods) if (method.equalsIgnoreCase(actual)) return;
        throw new MethodNotAllowedException(String.join(",", methods), "Use " + String.join(" or ", methods) + " for this endpoint.");
    }

    private String contentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static final class MethodNotAllowedException extends RuntimeException {
        private final String allowed;
        private MethodNotAllowedException(String allowed, String message) {
            super(message);
            this.allowed = allowed;
        }
    }
}
