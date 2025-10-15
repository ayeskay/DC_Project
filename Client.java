import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {

    // A helper method to print the current state of the game
    private static void printGameState(GameState state) {
        System.out.println("----------------------------------------");
        if (state == null) {
            System.out.println("Could not retrieve game state.");
            return;
        }
        System.out.println("Dealer's Hand: " + state.dealerHand);
        System.out.println("Your Hand: " + state.playerHand);
        System.out.println("\nServer Status: " + state.statusMessage);
        System.out.println("----------------------------------------");
    }

    public static void main(String[] args) {
        try {
            // Step 1: Connect to the RMI Registry
            Registry registry = LocateRegistry.getRegistry("localhost");

            // Step 2: Look up the Load Balancer
            System.out.println("Contacting Load Balancer...");
            LoadBalancerInterface lb = (LoadBalancerInterface) registry.lookup("BlackjackLoadBalancer");

            // Step 3: Ask the Load Balancer for an available game server
            String gameServerName = lb.getAvailableGameServer();
            System.out.println("Load Balancer assigned us to server: " + gameServerName);

            // Step 4: Look up the assigned Game Server
            GameInterface gameServer = (GameInterface) registry.lookup(gameServerName);
            System.out.println("✅ Successfully connected to the game server!");

            // --- Interactive Game Loop ---
            Scanner scanner = new Scanner(System.in);
            GameState currentState = gameServer.getGameState();

            while (true) {
                printGameState(currentState);

                // This logic mirrors your original client's flow
                if (currentState.statusMessage.contains("Please place your bet")) {
                    System.out.print("Enter your bet amount: ");
                    try {
                        int bet = Integer.parseInt(scanner.nextLine());
                        currentState = gameServer.placeBet(bet);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid bet. Please enter a number.");
                    }
                } else if (currentState.isPlayerTurn) {
                    System.out.print("Choose your action (hit / stand): ");
                    String action = scanner.nextLine().trim().toLowerCase();

                    if ("hit".equals(action)) {
                        currentState = gameServer.hit();
                    } else if ("stand".equals(action)) {
                        currentState = gameServer.stand();
                    } else {
                        System.out.println("Invalid action. Please enter 'hit' or 'stand'.");
                    }
                } else {
                    // Game is over, prompt for a new game or exit
                    System.out.print("Game Over. Play again? (yes / no): ");
                    String choice = scanner.nextLine().trim().toLowerCase();
                    if ("yes".equals(choice)) {
                        // In a real game, you would call a newGame() method.
                        // For this lab, we'll just reset by reconnecting.
                        System.out.println("Starting a new game...");
                        gameServerName = lb.getAvailableGameServer();
                        gameServer = (GameInterface) registry.lookup(gameServerName);
                        currentState = gameServer.getGameState();
                    } else {
                        System.out.println("Thanks for playing!");
                        break; // Exit the loop
                    }
                }
            }
            scanner.close();

        } catch (Exception e) {
            System.err.println("Client exception: " + e.getMessage());
            System.err.println("The server may be down. Please try again later.");
            e.printStackTrace();
        }
    }
}
