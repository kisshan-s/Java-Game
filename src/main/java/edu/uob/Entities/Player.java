package edu.uob.Entities;

import java.util.ArrayList;
import java.util.HashMap;

public class Player extends GameEntityCharacter{
    private HashMap<String, GameEntityArtefact> playerInventory;
    private String playerLocation;
    private int health = 3;
    private final String startingLocation;

    public Player(String name, String description, String startingLocation) {
        super(name, description);
        this.startingLocation = startingLocation;
        playerInventory = new HashMap<>();
    }

    public void addItemToInventory(GameEntityArtefact entity) {
        playerInventory.put(entity.getName(), entity);
    }

    public void removeItemFromInventory(String entityName){
        playerInventory.remove(entityName);
    }

    public GameEntityArtefact getItemFromInventory(String itemName){
        return playerInventory.get(itemName);
    }

    public boolean checkInventory(String entityName){
        return playerInventory.containsKey(entityName);
    }

    public HashMap<String, GameEntityArtefact> getPlayerInventory(){ return playerInventory; }

    public void setLocation(String location){ playerLocation = location; }

    public String getLocation(){ return playerLocation; }

    public int getHealth() {
        return health;
    }

    public void reduceHealth() { health--; }

    public void increaseHealth(){ if (health < 3) { health++; } }

    public void resetPlayer(){
        health = 3;
        setLocation(startingLocation);
        playerInventory.clear();
    }

}
