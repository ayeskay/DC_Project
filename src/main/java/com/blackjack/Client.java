package com.blackjack;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {

    private static void printGameState(GameState state) {
        System.out.println("----------------------------------------");
        if (state == null) {
            System.out.println("Could not retrieve game state.");
            return;
        }

        // Show dealer's hand
        if (state.isPlayerTurn && state.dealerHand.size() > 0) {
            // Hide dealer's second card if it's the player's turn
            System.out.println("Dealer's Hand: [" + state.dealerHand.get(0) + ", (Hidden)]");
        } else {
            // Show full dealer hand when game is over or not started
            System.out.println("Dealer's Hand: " + state.dealerHand + " (Value: " + state.getDealerValue() + ")");
        }

        // Show player's hand
        System.out.println("Your Hand: " + state.playerHand + " (Value: " + state.getPlayerValue() + ")");

        System.out.println("\nServer Status: " + state.statusMessage);
        System.out.println("----------------------------------------");
    }

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost"); // Connects to 1099

            System.out.println("Contacting Load Balancer...");
            LoadBalancerInterface lb = (LoadBalancerInterface) registry.lookup("LoadBalancer");

            String gameServerName = lb.getAvailableGameServer();
            System.out.println("Load Balancer assigned us to server: " + gameServerName);

            GameInterface gameServer = (GameInterface) registry.lookup(gameServerName);
            System.out.println("Successfully connected to the game server!");

            Scanner scanner = new Scanner(System.in);

            GameState currentState = gameServer.createNewGame();
            String myGameId = currentState.getId();
            System.out.println("Started new game. Game ID: " + myGameId);


            while (true) {
                printGameState(currentState);

                if (currentState.statusMessage.contains("Please place your bet")) {
                    System.out.print("Enter your bet amount: ");
                    try {
                        int bet = Integer.parseInt(scanner.nextLine());
                        if (bet > 0) {
                            currentState = gameServer.placeBet(myGameId, bet);
                        } else {
                            System.out.println("Bet must be a positive number.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid bet. Please enter a number.");
                    }
                } else if (currentState.isPlayerTurn) {
                    System.out.print("Choose your action (hit / stand): ");
                    String action = scanner.nextLine().trim().toLowerCase();

                    if ("hit".equals(action)) {
                        currentState = gameServer.hit(myGameId);
                    } else if ("stand".equals(action)) {
                        currentState = gameServer.stand(myGameId);
                    } else {
                        System.out.println("Invalid action. Please enter 'hit' or 'stand'.");
                    }
                } else {
                    // Game is over (isPlayerTurn is false and not waiting for bet)
                    System.out.print("Game Over. Play again? (yes / no): ");
                    String choice = scanner.nextLine().trim().toLowerCase();
                    if ("yes".equals(choice)) {
                        System.out.println("Starting a new game...");
                        // Re-contact load balancer for fault tolerance
                        gameServerName = lb.getAvailableGameServer();
                        System.out.println("Load Balancer assigned us to server: " + gameServerName);
                        gameServer = (GameInterface) registry.lookup(gameServerName);

                        currentState = gameServer.createNewGame();
                        myGameId = currentState.getId();
                    } else {
                        System.out.println("Thanks for playing!");
                        break;
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