package com.blackjack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public String id;

    public List<String> deck;
    public List<String> playerHand;
    public List<String> dealerHand;
    public int playerBet;
    public boolean isPlayerTurn;
    public String statusMessage;

    public GameState() {
        this.id = UUID.randomUUID().toString();
        this.deck = new ArrayList<>();
        this.playerHand = new ArrayList<>();
        this.dealerHand = new ArrayList<>();
        this.playerBet = 0;
        this.isPlayerTurn = true;
        this.statusMessage = "New game started. Please place your bet.";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
