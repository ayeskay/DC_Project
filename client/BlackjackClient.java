import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class BlackjackClient {
    private static BlackjackGame game;
    private static Scanner scanner;
    private static int sessionId = -1;
    private static String currentTable = null;
    
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            game = (BlackjackGame) registry.lookup("BlackjackService");
            scanner = new Scanner(System.in);
            
            System.out.println("=== Welcome to Multi-Table Blackjack ===");
            
            while (true) {
                if (sessionId == -1) {
                    showMainMenu();
                } else {
                    showTableMenu();
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void showMainMenu() {
        System.out.println("\n=== MAIN MENU ===");
        System.out.println("1. View available tables");
        System.out.println("2. Join a table");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");
        
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1":
                viewTables();
                break;
            case "2":
                joinTable();
                break;
            case "3":
                System.out.println("Thanks for playing!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option. Please try again.");
        }
    }
    
    private static void viewTables() {
        try {
            List<String> tables = game.getAvailableTables();
            System.out.println("\n=== AVAILABLE TABLES ===");
            for (int i = 0; i < tables.size(); i++) {
                System.out.println((i + 1) + ". " + tables.get(i));
            }
        } catch (Exception e) {
            System.out.println("Error retrieving tables: " + e.getMessage());
        }
    }
    
    private static void joinTable() {
        try {
            List<String> tables = game.getAvailableTables();
            if (tables.isEmpty()) {
                System.out.println("No tables available.");
                return;
            }
            
            System.out.println("\n=== JOIN TABLE ===");
            for (int i = 0; i < tables.size(); i++) {
                System.out.println((i + 1) + ". " + tables.get(i));
            }
            
            System.out.print("Select table (1-" + tables.size() + "): ");
            String input = scanner.nextLine();
            
            try {
                int tableIndex = Integer.parseInt(input) - 1;
                if (tableIndex >= 0 && tableIndex < tables.size()) {
                    String tableInfo = tables.get(tableIndex);
                    String tableName = tableInfo.split(" ")[0]; // Extract table name
                    
                    sessionId = game.joinTable(tableName);
                    currentTable = tableName;
                    System.out.println("Joined table: " + tableName);
                    System.out.println("Session ID: " + sessionId);
                } else {
                    System.out.println("Invalid table selection.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        } catch (Exception e) {
            System.out.println("Error joining table: " + e.getMessage());
        }
    }
    
    private static void showTableMenu() {
        try {
            if (game.getGameStatus(sessionId).equals("Invalid session")) {
                System.out.println("Session expired. Returning to main menu.");
                sessionId = -1;
                currentTable = null;
                return;
            }
            
            System.out.println("\n=== TABLE: " + currentTable + " ===");
            System.out.println("Your points: " + game.getPlayerPoints(sessionId));
            System.out.println("1. Place bet and start new game");
            System.out.println("2. View current game");
            System.out.println("3. Leave table");
            System.out.print("Choose an option: ");
            
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    placeBetAndPlay();
                    break;
                case "2":
                    viewCurrentGame();
                    break;
                case "3":
                    leaveTable();
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        } catch (Exception e) {
            System.out.println("Error in table menu: " + e.getMessage());
        }
    }
    
    private static void placeBetAndPlay() {
        try {
            // Get table info for min/max bets
            Map<String, Object> tableInfo = game.getTableInfo(currentTable);
            if (tableInfo != null) {
                int minBet = (Integer) tableInfo.get("minBet");
                int maxBet = (Integer) tableInfo.get("maxBet");
                
                System.out.println("\n=== PLACE BET ===");
                System.out.println("Minimum bet: " + minBet);
                System.out.println("Maximum bet: " + maxBet);
                System.out.println("Your points: " + game.getPlayerPoints(sessionId));
                System.out.print("Enter bet amount (" + minBet + "-" + maxBet + "): ");
                
                try {
                    int bet = Integer.parseInt(scanner.nextLine());
                    game.placeBet(sessionId, bet);
                    game.startNewGame(sessionId);
                    playGame();
                } catch (NumberFormatException e) {
                    System.out.println("Invalid bet amount.");
                }
            }
        } catch (Exception e) {
            System.out.println("Error placing bet: " + e.getMessage());
        }
    }
    
    private static void playGame() {
        try {
            while (game.getGameStatus(sessionId).contains("Your turn") || 
                   game.getGameStatus(sessionId).contains("Hit or Stand")) {
                
                System.out.println("\n--- Your Turn ---");
                System.out.println("Your hand: " + game.getPlayerHand(sessionId));
                System.out.println("Dealer's hand: " + game.getDealerHand(sessionId));
                System.out.println("Your points: " + game.getPlayerPoints(sessionId));
                System.out.print("Do you want to (h)it or (s)tand? ");
                String choice = scanner.nextLine();

                if ("h".equalsIgnoreCase(choice)) {
                    game.hit(sessionId);
                } else if ("s".equalsIgnoreCase(choice)) {
                    game.stand(sessionId);
                    break;
                } else {
                    System.out.println("Invalid choice. Please enter 'h' or 's'.");
                }
            }

            // Show final result
            System.out.println("\n--- Game Over ---");
            System.out.println("Your final hand: " + game.getPlayerHand(sessionId));
            System.out.println("Dealer's final hand: " + game.getDealerHand(sessionId));
            System.out.println("Result: " + game.getGameStatus(sessionId));
            System.out.println("Your points: " + game.getPlayerPoints(sessionId));
            
            // Check if player is out of points
            if (game.getPlayerPoints(sessionId) <= 0) {
                handleBrokePlayer();
            }
        } catch (Exception e) {
            System.out.println("Error during game: " + e.getMessage());
        }
    }
    
    private static void handleBrokePlayer() {
        System.out.println("\n💀 You're out of points!");
        System.out.println("Options:");
        System.out.println("1. Buy in more points");
        System.out.println("2. Leave table");
        System.out.print("Choose (1-2): ");
        
        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                buyInMorePoints();
                break;
            case "2":
                leaveTable();
                break;
            default:
                System.out.println("Invalid choice. Leaving table.");
                leaveTable();
        }
    }
    
    private static void buyInMorePoints() {
        try {
            System.out.print("Enter amount to buy in: ");
            try {
                int amount = Integer.parseInt(scanner.nextLine());
                if (amount > 0) {
                    game.buyIn(sessionId, amount);
                    System.out.println("Successfully bought in " + amount + " points.");
                    System.out.println("New balance: " + game.getPlayerPoints(sessionId));
                } else {
                    System.out.println("Invalid amount.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount.");
            }
        } catch (Exception e) {
            System.out.println("Error buying in: " + e.getMessage());
        }
    }
    
    private static void viewCurrentGame() {
        try {
            System.out.println("\n--- Current Game ---");
            System.out.println("Your hand: " + game.getPlayerHand(sessionId));
            System.out.println("Dealer's hand: " + game.getDealerHand(sessionId));
            System.out.println("Status: " + game.getGameStatus(sessionId));
            System.out.println("Your points: " + game.getPlayerPoints(sessionId));
        } catch (Exception e) {
            System.out.println("Error viewing game: " + e.getMessage());
        }
    }
    
    private static void leaveTable() {
        try {
            game.leaveTable(sessionId);
            System.out.println("Left table: " + currentTable);
            sessionId = -1;
            currentTable = null;
        } catch (Exception e) {
            System.out.println("Error leaving table: " + e.getMessage());
        }
    }
}