package com.blackjack;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameInterface extends Remote {
    GameState createNewGame() throws RemoteException;
    GameState getGameState(String gameId) throws RemoteException;
    GameState placeBet(String gameId, int bet) throws RemoteException;
    GameState hit(String gameId) throws RemoteException;
    GameState stand(String gameId) throws RemoteException;
    boolean ping() throws RemoteException;
}
