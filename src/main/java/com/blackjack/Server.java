package com.blackjack;

import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Properties;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;

public class Server implements GameInterface {

    private static String ENDPOINT;
    private static String KEY;
    private static final String DATABASE_ID = "BlackjackDB";
    private static final String CONTAINER_ID = "Games";

    private CosmosClient cosmosClient;
    private CosmosDatabase database;
    private CosmosContainer container;

    public Server() {
        try {
            loadConfiguration();
            this.cosmosClient = new CosmosClientBuilder()
                    .endpoint(ENDPOINT)
                    .key(KEY)
                    .buildClient();
            this.database = cosmosClient.getDatabase(DATABASE_ID);
            this.container = database.getContainer(CONTAINER_ID);
            System.out.println("New Server instance created and connected to Cosmos DB.");
        } catch (Exception e) {
            System.err.println("Failed to initialize Server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void loadConfiguration() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = Server.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Sorry, unable to find config.properties. Make sure it is in src/main/resources");
            }
            properties.load(input);
            ENDPOINT = properties.getProperty("COSMOS_DB_ENDPOINT").trim();
            KEY = properties.getProperty("COSMOS_DB_KEY").trim();

            if (ENDPOINT == null || KEY == null || ENDPOINT.isEmpty() || KEY.isEmpty() || ENDPOINT.equals("YOUR_URI_HERE")) {
                throw new RuntimeException("COSMOS_DB_ENDPOINT or COSMOS_DB_KEY is not set correctly in config.properties");
            }
        }
    }

    private void initializeGame(GameState gameState) {
        createDeck(gameState);
        shuffleDeck(gameState);
    }

    private void createDeck(GameState gameState) {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace"};
        for (String suit : suits) {
            for (String rank : ranks) {
                gameState.deck.add(rank + " of " + suit);
            }
        }
    }

    private void shuffleDeck(GameState gameState) {
        Collections.shuffle(gameState.deck);
    }

    @Override
    public GameState createNewGame() throws RemoteException {
        GameState newGame = new GameState();
        initializeGame(newGame);
        
        container.createItem(newGame);
        System.out.println("Created new game: " + newGame.getId());
        
        return newGame;
    }

    private GameState readState(String gameId) {
        CosmosItemResponse<GameState> response = container.readItem(
            gameId, new PartitionKey(gameId), GameState.class);
        return response.getItem();
    }
    
    private void saveState(GameState gameState) {
        container.upsertItem(gameState);
    }

    @Override
    public GameState placeBet(String gameId, int bet) throws RemoteException {
        System.out.println("Received bet: " + bet + " for game: " + gameId);
        GameState gameState = readState(gameId);
        
        gameState.playerBet = bet;
        gameState.statusMessage = "Bet of " + bet + " placed. Your turn.";
        
        saveState(gameState);
        return gameState;
    }

    @Override
    public GameState hit(String gameId) throws RemoteException {
        System.out.println("Player hits for game: " + gameId);
        GameState gameState = readState(gameId);
        
        gameState.statusMessage = "Player hits. Current hand value is X.";
        
        saveState(gameState);
        return gameState;
    }

    @Override
    public GameState stand(String gameId) throws RemoteException {
        System.out.println("Player stands for game: " + gameId);
        GameState gameState = readState(gameId);
        
        gameState.isPlayerTurn = false;
        gameState.statusMessage = "Player stands. Dealer's turn.";
        
        saveState(gameState);
        return gameState;
    }

    @Override
    public GameState getGameState(String gameId) throws RemoteException { 
        return readState(gameId);
    }
    
    @Override
    public boolean ping() throws RemoteException { return true; }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Server <RMI_Server_Name>");
            return;
        }
        String serverName = args[0];

        try {
            Server server = new Server();
            GameInterface stub = (GameInterface) UnicastRemoteObject.exportObject(server, 0);
            
            Registry registry = LocateRegistry.getRegistry("localhost");
            
            registry.bind(serverName, stub);
            System.out.println("Game Server '" + serverName + "' is ready.");
            
            LoadBalancerInterface lb = (LoadBalancerInterface) registry.lookup("BlackjackLoadBalancer");
            lb.registerGameServer(serverName);
            System.out.println("'" + serverName + "' registered with Load Balancer.");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
