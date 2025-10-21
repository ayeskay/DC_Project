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
        System.out.println("Dealer's Hand: " + state.dealerHand);
        System.out.println("Your Hand: " + state.playerHand);
        System.out.println("\nServer Status: " + state.statusMessage);
        System.out.println("----------------------------------------");
    }

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");

            System.out.println("Contacting Load Balancer...");
            LoadBalancerInterface lb = (LoadBalancerInterface) registry.lookup("BlackjackLoadBalancer");

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
                        currentState = gameServer.placeBet(myGameId, bet);
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
                    System.out.print("Game Over. Play again? (yes / no): ");
                    String choice = scanner.nextLine().trim().toLowerCase();
                    if ("yes".equals(choice)) {
                        System.out.println("Starting a new game...");
                        gameServerName = lb.getAvailableGameServer();
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
