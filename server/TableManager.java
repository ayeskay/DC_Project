import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

public class TableManager {
    private Map<String, BlackjackTable> tables;
    
    public TableManager() {
        tables = new ConcurrentHashMap<>();
        // Create some default tables
        createTable("High Rollers", 100, 1000);
        createTable("Standard", 10, 100);
        createTable("Beginners", 1, 10);
    }
    
    public synchronized void createTable(String tableName, int minBet, int maxBet) {
        if (!tables.containsKey(tableName)) {
            tables.put(tableName, new BlackjackTable(tableName, minBet, maxBet));
        }
    }
    
    public List<String> getAvailableTables() {
        List<String> tableList = new ArrayList<>();
        for (Map.Entry<String, BlackjackTable> entry : tables.entrySet()) {
            BlackjackTable table = entry.getValue();
            tableList.add(entry.getKey() + " (Min: " + table.getMinBet() + 
                         ", Max: " + table.getMaxBet() + 
                         ", Players: " + table.getPlayerCount() + ")");
        }
        return tableList;
    }
    
    public BlackjackTable getTable(String tableName) {
        return tables.get(tableName);
    }
    
    public Map<String, Object> getTableInfo(String tableName) {
        BlackjackTable table = tables.get(tableName);
        if (table == null) {
            return null;
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("name", table.getTableName());
        info.put("minBet", table.getMinBet());
        info.put("maxBet", table.getMaxBet());
        info.put("playerCount", table.getPlayerCount());
        info.put("players", table.getPlayerNames());
        return info;
    }
    
    public List<String> getPlayersAtTable(String tableName) {
        BlackjackTable table = tables.get(tableName);
        if (table == null) {
            return new ArrayList<>();
        }
        return table.getPlayerNames();
    }
}