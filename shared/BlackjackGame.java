import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface BlackjackGame extends Remote {
    // Server/Table Management
    List<String> getAvailableTables() throws RemoteException;
    int joinTable(String tableName) throws RemoteException;
    void leaveTable(int sessionId) throws RemoteException;
    
    // Player Management
    void registerPlayer(String playerName, int initialPoints) throws RemoteException;
    int getPlayerPoints(int sessionId) throws RemoteException;
    void buyIn(int sessionId, int points) throws RemoteException;
    
    // Game Operations
    void startNewGame(int sessionId) throws RemoteException;
    List<Card> getPlayerHand(int sessionId) throws RemoteException;
    List<Card> getDealerHand(int sessionId) throws RemoteException;
    String getGameStatus(int sessionId) throws RemoteException;
    void placeBet(int sessionId, int amount) throws RemoteException;
    void hit(int sessionId) throws RemoteException;
    void stand(int sessionId) throws RemoteException;
    
    // Table Information
    Map<String, Object> getTableInfo(String tableName) throws RemoteException;
    List<String> getPlayersAtTable(String tableName) throws RemoteException;
}