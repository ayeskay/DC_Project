import java.rmi.Remote;
import java.rmi.RemoteException;

public interface LoadBalancerInterface extends Remote {
    /**
     * Called by a Game Server to add itself to the list of available servers.
     * @param serverName The unique RMI name of the game server (e.g., "GameServer1").
     */
    void registerGameServer(String serverName) throws RemoteException;

    /**
     * Called by a Client to get the name of a game server to connect to.
     * @return The RMI name of an available game server.
     */
    String getAvailableGameServer() throws RemoteException;
}
