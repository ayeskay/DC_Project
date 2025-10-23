package com.blackjack;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class LoadBalancer implements LoadBalancerInterface {
    private List<String> availableServers;
    private int nextServerIndex;

    public LoadBalancer() {
        availableServers = new ArrayList<>();
        nextServerIndex = 0;
    }

    @Override
    public synchronized void registerGameServer(String serverName) throws RemoteException {
        if (!availableServers.contains(serverName)) {
            availableServers.add(serverName);
            System.out.println("Registered new game server: " + serverName);
        }
    }

    @Override
    public synchronized String getAvailableGameServer() throws RemoteException {
        if (availableServers.isEmpty()) {
            throw new RemoteException("No game servers are currently available.");
        }
        String server = availableServers.get(nextServerIndex);
        nextServerIndex = (nextServerIndex + 1) % availableServers.size();
        System.out.println("Redirecting client to -> " + server);
        return server;
    }

    public static void main(String[] args) {
        try {
            LoadBalancer lb = new LoadBalancer();
            LoadBalancerInterface stub = (LoadBalancerInterface) UnicastRemoteObject.exportObject(lb, 0);

            // --- THIS IS THE FIX ---
            // Get the registry created by 'rmiregistry' instead of creating a new one
            Registry registry = LocateRegistry.getRegistry(1099);
            // --- END OF FIX ---

            registry.bind("LoadBalancer", stub);

            System.out.println("Load Balancer is ready.");
        } catch (Exception e) {
            System.err.println("Load Balancer exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}