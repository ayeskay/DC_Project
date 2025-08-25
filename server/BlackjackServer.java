import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlackjackServer extends UnicastRemoteObject implements BlackjackGame {
    private TableManager tableManager;
    private Map<Integer, String> sessionToTable;
    private Map<String, Player> registeredPlayers;
    
    public BlackjackServer() throws RemoteException {
        super();
        tableManager = new TableManager();
        sessionToTable = new ConcurrentHashMap<>();
        registeredPlayers = new ConcurrentHashMap<>();
    }
    
    @Override
    public List<String> getAvailableTables() throws RemoteException {
        return tableManager.getAvailableTables();
    }
    
    @Override
    public int joinTable(String tableName) throws RemoteException {
        BlackjackTable table = tableManager.getTable(tableName);
        if (table == null) {
            throw new RemoteException("Table not found: " + tableName);
        }
        // Note: In a real implementation, you'd associate this with a registered player
        // For simplicity, we'll create a default player
        Player player = new Player("Player" + System.currentTimeMillis(), 1000);
        int sessionId = table.addPlayer(player);
        sessionToTable.put(sessionId, tableName);
        return sessionId;
    }
    
    @Override
    public void leaveTable(int sessionId) throws RemoteException {
        String tableName = sessionToTable.get(sessionId);
        if (tableName != null) {
            BlackjackTable table = tableManager.getTable(tableName);
            if (table != null) {
                table.removePlayer(sessionId);
            }
            sessionToTable.remove(sessionId);
        }
    }
    
    @Override
    public void registerPlayer(String playerName, int initialPoints) throws RemoteException {
        if (!registeredPlayers.containsKey(playerName)) {
            registeredPlayers.put(playerName, new Player(playerName, initialPoints));
        }
    }
    
    @Override
    public int getPlayerPoints(int sessionId) throws RemoteException {
        String tableName = sessionToTable.get(sessionId);
        if (tableName != null) {
            BlackjackTable table = tableManager.getTable(tableName);
            if (table != null) {
                return table.getPlayerPoints(sessionId);
            }
        }
        return 0;
    }
    
    @Override
    public void buyIn(int sessionId, int points) throws RemoteException {
        String tableName = sessionToTable.get(sessionId);
        if (tableName != null) {
            BlackjackTable table = tableManager.getTable(tableName);
            if (table != null) {
                table.buyIn(sessionId, points);
            }
        }
    }
    
    @Override
    public void startNewGame(int sessionId) throws RemoteException {
        String tableName = sessionToTable.get(sessionId);
        if (tableName != null) {
            BlackjackTable table = tableManager.getTable(tableName);
            if (table != null) {
                table.startNewGame(sessionId);
            }
        }
    }
    
    @Override
    public List<Card> getPlayerHand(int sessionId) throws RemoteException {
        String tableName = sessionToTable.get(sessionId);
        if (tableName != null) {
            BlackjackTable table = tableManager.getTable(tableName);
            if (table != null) {
                return table.getPlayerHand(sessionId);
            }
        }
        return new ArrayList<>();
    }
    
    @Override
    public List<Card> getDealerHand(int sessionId) throws RemoteException {
        String tableName = sessionToTable.get(sessionId);
        if (tableName != null) {
            BlackjackTable table = tableManager.getTable(tableName);
            if (table != null) {
                return table.getDealerHand(sessionId);
            }
        }
        return new ArrayList<>();
    }
    
    @Override
    public String getGameStatus(int sessionId) throws RemoteException {
        String tableName = sessionToTable.get(sessionId);
        if (tableName != null) {
            BlackjackTable table = tableManager.getTable(tableName);
            if (table != null) {
                return table.getGameStatus(sessionId);
            }
        }
        return "Not at a table";
    }
    
    @Override
    public void placeBet(int sessionId, int amount) throws RemoteException {
        String tableName = sessionToTable.get(sessionId);
        if (tableName != null) {
            BlackjackTable table = tableManager.getTable(tableName);
            if (table != null) {
                table.placeBet(sessionId, amount);
            }
        }
    }
    
    @Override
    public void hit(int sessionId) throws RemoteException {
        String tableName = sessionToTable.get(sessionId);
        if (tableName != null) {
            BlackjackTable table = tableManager.getTable(tableName);
            if (table != null) {
                table.hit(sessionId);
            }
        }
    }
    
    @Override
    public void stand(int sessionId) throws RemoteException {
        String tableName = sessionToTable.get(sessionId);
        if (tableName != null) {
            BlackjackTable table = tableManager.getTable(tableName);
            if (table != null) {
                table.stand(sessionId);
            }
        }
    }
    
    @Override
    public Map<String, Object> getTableInfo(String tableName) throws RemoteException {
        return tableManager.getTableInfo(tableName);
    }
    
    @Override
    public List<String> getPlayersAtTable(String tableName) throws RemoteException {
        return tableManager.getPlayersAtTable(tableName);
    }
    
    public static void main(String[] args) {
        try {
            BlackjackServer server = new BlackjackServer();
            Registry registry = LocateRegistry.getRegistry(1099);
            registry.rebind("BlackjackService", server);
            System.out.println("Blackjack Server is ready with multi-table support.");
            System.out.println("Available tables created:");
            System.out.println("- High Rollers (100-1000 points)");
            System.out.println("- Standard (10-100 points)");
            System.out.println("- Beginners (1-10 points)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
