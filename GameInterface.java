import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameInterface extends Remote {
    // Returns the current state of the game
    GameState getGameState() throws RemoteException;

    // Player actions
    GameState placeBet(int bet) throws RemoteException;
    GameState hit() throws RemoteException;
    GameState stand() throws RemoteException;

    /**
     * A simple method for the backup server to check if the primary is alive.
     * @return true if the server is responsive.
     */
    boolean ping() throws RemoteException;
}
