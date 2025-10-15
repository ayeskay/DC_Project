import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BackupInterface extends Remote {
    /**
     * The method the Primary Server calls to send the latest game state.
     * @param newState The state object to be replicated.
     */
    void updateState(GameState newState) throws RemoteException;
}
