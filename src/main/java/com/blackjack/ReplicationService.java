package com.blackjack;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ReplicationService {
    private final int replicaId;
    private final List<String> replicas; // host:port entries
    private final int listenPort;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean running = true;
    private final GameManager gameManager; // Reference to the server's game manager

    /**
     * This is the new constructor that Server.java needs.
     */
    public ReplicationService(Properties p, GameManager gm) throws Exception {
        this.replicaId = Integer.parseInt(p.getProperty("replica.id").trim());
        this.listenPort = Integer.parseInt(p.getProperty("listen.port").trim());
        String repList = p.getProperty("replicas").trim();
        this.replicas = Arrays.asList(repList.split("\\s*,\\s*"));
        this.gameManager = gm; // Set the GameManager reference
        startListener();
        log("ReplicationService initialized. replicaId=" + replicaId + " listenPort=" + listenPort);
    }

    private void log(String s) { System.out.println("[ReplicationService-" + replicaId + "] " + s); }

    private void startListener() {
        executor.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
                log("Listening for state updates on port " + listenPort);
                while (running) {
                    Socket socket = serverSocket.accept();
                    executor.submit(() -> handleIncoming(socket));
                }
            } catch (Exception e) {
                if(running) log("Listener error: " + e.getMessage());
            }
        });
    }

    private void handleIncoming(Socket socket) {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            Object obj = ois.readObject();
            if (obj instanceof GameState) {
                GameState state = (GameState) obj;
                log("Received GameState update. id=" + state.getId() + " status=" + state.statusMessage);
                
                // This is the crucial change:
                // Update the in-memory state via the GameManager
                // GameManager will also handle persistence
                if (gameManager != null) {
                    gameManager.setGameState(state);
                    log("Applied state update to local GameManager.");
                } else {
                    log("Warning: GameManager is null, cannot apply state update.");
                }

            } else {
                log("Received unknown object: " + obj);
            }
        } catch (Exception e) {
            log("Error handling incoming connection: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    public void broadcastState(GameState state) {
        log("Broadcasting state for game " + state.getId() + " to " + replicas.size() + " replicas.");
        for (String entry : replicas) {
            executor.submit(() -> {
                try {
                    String[] parts = entry.split(":");
                    if (parts.length != 2) return;
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);

                    // Skip sending to self
                    if ((host.equals("localhost") || host.equals("127.0.0.1")) && port == listenPort) {
                        return;
                    }

                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress(host, port), 2000); // 2 sec timeout
                        ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                        oos.writeObject(state);
                        oos.flush();
                        log("Sent state to " + host + ":" + port);
                    }
                } catch (Exception e) {
                    log("Failed to send to " + entry + " - " + e.getMessage());
                }
            });
        }
    }

    public void shutdown() { 
        running = false; 
        executor.shutdownNow(); 
        log("Shutdown complete.");
    }
}