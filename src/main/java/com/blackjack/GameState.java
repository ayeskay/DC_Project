package com.blackjack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    public List<Card> deck;
    public List<Card> playerHand;
    public List<Card> dealerHand;
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

    // convenience for older code that used getGameId()
    public String getGameId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Calculates the value of a given hand, handling Aces as 1 or 11.
     */
    public int getHandValue(List<Card> hand) {
        int value = 0;
        int aces = 0;
        for (Card card : hand) {
            value += card.getValue();
            if (card.getValue() == 11) { // Ace
                aces++;
            }
        }
        // Handle Aces: if value > 21, change Ace from 11 to 1
        while (value > 21 && aces > 0) {
            value -= 10;
            aces--;
        }
        return value;
    }

    public int getPlayerValue() {
        return getHandValue(playerHand);
    }

    public int getDealerValue() {
        return getHandValue(dealerHand);
    }

    @Override
    public String toString() {
        return "GameState{id=" + id + ", playerHand=" + playerHand + ", dealerHand=" + dealerHand +
                ", bet=" + playerBet + ", status='" + statusMessage + "'}";
    }
}