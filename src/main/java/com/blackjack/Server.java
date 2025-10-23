package com.blackjack;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.rmi.RemoteException;

public class Server implements GameInterface {
    private final GameManager gameManager;
    private final ReplicationService replicationService; // optional: used if configured
    private final boolean isLeader;

    public Server(boolean isLeader, ReplicationService repSvc, GameManager gMgr) {
        this.isLeader = isLeader;
        this.replicationService = repSvc;
        this.gameManager = gMgr; // Use the GameManager passed from main
    }

    // RMI methods - must throw RemoteException
    @Override
    public GameState createNewGame() throws RemoteException {
        System.out.println("Request: createNewGame");
        GameState gs = gameManager.createNewGame();
        if (isLeader && replicationService != null) replicationService.broadcastState(gs);
        return gs;
    }

    @Override
    public GameState getGameState(String gameId) throws RemoteException {
        System.out.println("Request: getGameState for " + gameId);
        return gameManager.getGameState(gameId);
    }

    @Override
    public GameState placeBet(String gameId, int bet) throws RemoteException {
        System.out.println("Request: placeBet for " + gameId);
        GameState gs = gameManager.placeBet(gameId, bet);
        if (isLeader && replicationService != null && gs != null) replicationService.broadcastState(gs);
        return gs;
    }

    @Override
    public GameState hit(String gameId) throws RemoteException {
        System.out.println("Request: hit for " + gameId);
        GameState gs = gameManager.hit(gameId);
        if (isLeader && replicationService != null && gs != null) replicationService.broadcastState(gs);
        return gs;
    }

    @Override
    public GameState stand(String gameId) throws RemoteException {
        System.out.println("Request: stand for " + gameId);
        GameState gs = gameManager.stand(gameId);
        if (isLeader && replicationService != null && gs != null) replicationService.broadcastState(gs);
        return gs;
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    // main: start RMI server, register with load balancer if present, start replication service
    public static void main(String[] args) {
        try {
            // Allow properties file to be specified as an argument
            String propsFile = "src/main/resources/replication.properties"; // default
            if (args.length > 0) {
                propsFile = args[0];
                System.out.println("Loading properties from: " + propsFile);
            } else {
                System.out.println("Loading default properties from: " + propsFile);
            }

            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(propsFile)) {
                props.load(fis);
            } catch (Exception e) {
                System.out.println("Warning: Could not load properties file. Using defaults.");
                // if no replication props, continue with defaults
            }

            int replicaId = Integer.parseInt(props.getProperty("replica.id", "1").trim());
            int leaderId = Integer.parseInt(props.getProperty("leader.id", "1").trim());
            boolean isLeader = (replicaId == leaderId);

            // Create GameManager and load state from its persistent file
            File stateFile = new File("replica_state_" + replicaId + ".ser");
            GameManager gameManager = new GameManager(stateFile);

            ReplicationService repSvc = null;
            try {
                if (props.getProperty("replicas") != null) {
                    // Pass the GameManager to the ReplicationService
                    repSvc = new ReplicationService(props, gameManager);
                }
            } catch (Exception e) {
                System.out.println("Warning: replication service not started: " + e.getMessage());
            }

            Server obj = new Server(isLeader, repSvc, gameManager);
            GameInterface stub = (GameInterface) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = null;
            try {
                 registry = LocateRegistry.getRegistry(1099);
                 registry.list(); // Test connection
            } catch (Exception e) {
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("Created new RMI registry on 1099");
            }
            
            // bind with a unique name
            String serverName = "GameServer-" + replicaId;
            registry.rebind(serverName, stub);
            System.out.println("Game server bound as: " + serverName + " (leader=" + isLeader + ")");

            // Try to register with load balancer if it exists
            try {
                LoadBalancerInterface lb = (LoadBalancerInterface) registry.lookup("LoadBalancer");
                lb.registerGameServer(serverName);
                System.out.println("Registered with load balancer.");
            } catch (Exception e) {
                System.out.println("Load balancer not found or registration failed: " + e.getMessage());
                System.out.println("Note: LoadBalancer must be running before servers.");
            }

            System.out.println("Server " + serverName + " is ready.");
            // keep server alive
            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}