package com.blackjack;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface LoadBalancerInterface extends Remote {
    void registerGameServer(String serverName) throws RemoteException;
    String getAvailableGameServer() throws RemoteException;
}
