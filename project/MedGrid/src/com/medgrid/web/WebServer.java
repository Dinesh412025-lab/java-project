package com.medgrid.web;

import com.medgrid.agent.DispatchAgent;
import com.medgrid.routing.Graph;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WebServer {
    private HttpServer server;
    private final int port;
    private final DispatchAgent dispatchAgent;
    private final Graph graph;

    public WebServer(int port, DispatchAgent dispatchAgent, Graph graph) {
        this.port = port;
        this.dispatchAgent = dispatchAgent;
        this.graph = graph;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/dashboard", new DashboardHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/dispatch", new DispatchHandler(dispatchAgent, graph));
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/ai2", new AI2Handler(graph));
        server.createContext("/api/triage", new TriageApiHandler());
        
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Web server started on http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
