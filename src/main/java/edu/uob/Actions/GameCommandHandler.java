package edu.uob.Actions;

import edu.uob.Entities.GameEntityLocation;
import edu.uob.Entities.Player;

import java.util.*;

public class GameCommandHandler {
    protected final List<String> basicCommands = Arrays.asList("goto", "look", "inv", "inventory", "drop", "get", "health");
    protected HashMap<String, GameEntityLocation> gameLocations;
    protected HashMap<String, HashSet<GameAction>> gameActions;
    protected Player player;
    protected HashMap<String, Player> players;
    protected final List<String> allEntities;

    public GameCommandHandler(HashMap<String, GameEntityLocation> gameLocations, HashMap<String, HashSet<GameAction>> gameActions, List<String> allEntities, HashMap<String, Player> players) {
        this.gameLocations = gameLocations;
        this.gameActions = gameActions;
        this.allEntities = allEntities;
        this.players = players;
    }

    /**
     * Checks player message for basic or advanced command and returns appropriate message
     *
     * @param playerMessage message received from the player through the client
     * @param player        current player object
     * @return result of the command the player performed
     * @throws IllegalArgumentException used to catch any invalid commands given by the player
     */
    public String handleCommand(String playerMessage, Player player) throws IllegalArgumentException {
        this.player = player;

        AdvancedCommandHandler handler = new AdvancedCommandHandler(gameLocations, gameActions, allEntities, players);
        List<String> tokenisedPlayerMessage = Arrays.asList(playerMessage.split("\\s+"));

        String commandWord = findBasicCommandInPlayerMessage(tokenisedPlayerMessage);
        SortedSet<String> triggersInPlayerMessage = handler.findTriggersInPlayerMessage(tokenisedPlayerMessage);

        if (commandWord.isEmpty() && triggersInPlayerMessage.isEmpty()) {
            throw new IllegalArgumentException("Sorry! " + player.getName() + " doesn't know what to do");
        }

        if (!commandWord.isEmpty() && !triggersInPlayerMessage.isEmpty()) {
            throw new IllegalArgumentException("Pick one command, it's hard to do two things at once :(");
        }

        if (triggersInPlayerMessage.isEmpty()) {
            return handleBasicCommand(commandWord, tokenisedPlayerMessage);
        } else return handler.handleAdvancedCommand(triggersInPlayerMessage, tokenisedPlayerMessage, player);
    }

    /**
     * Consists essentially of a switch statement that calls the appropriate method based on the command word
     *
     * @param commandWord        basic command found in the player message
     * @param tokenisedPlayerMessage player message split by spaces
     * @return result of command
     */
    public String handleBasicCommand(String commandWord, List<String> tokenisedPlayerMessage) {
        switch (commandWord) {
            case "look":
                return handleLook(tokenisedPlayerMessage);
            case "inv":
            case "inventory":
                return handleInv(tokenisedPlayerMessage);
            case "get":
                return handleGet(tokenisedPlayerMessage);
            case "drop":
                return handleDrop(tokenisedPlayerMessage);
            case "goto":
                return handleGoto(tokenisedPlayerMessage);
            case "health":
                return handleHealth(tokenisedPlayerMessage);
            default:
                throw new IllegalArgumentException("Sorry! " + player.getName() + " doesn't know what to do");
        }
    }

    /**
     * method to execute the 'look' command
     *
     * @return list of player's current location, as well as all GameEntity objects in the location
     */
    public String handleLook(List<String> tokenisedPlayerMessage) {
        GameEntityLocation location = gameLocations.get(player.getLocation());

        if (detectExtraEntities(tokenisedPlayerMessage, "look")) {
            throw new IllegalArgumentException("Error: extraneous entities detected in command!");
        } else return getEntitiesInView(location).toString();
    }

    /**
     * Method which executes the 'get' command
     *
     * @param tokenisedPlayerMessage player message split into individual words
     * @return message to be returned to the player upon successful execution of the command
     * @throws IllegalArgumentException if the player tries to 'get' more than one item or if the item doesn't exist
     */
    public String handleGet(List<String> tokenisedPlayerMessage) throws IllegalArgumentException {
        GameEntityLocation location = gameLocations.get(player.getLocation());
        String itemToGet = findItemToGet(tokenisedPlayerMessage, location);

        player.addItemToInventory(location.getArtefacts().get(itemToGet));
        location.getArtefacts().remove(itemToGet);

        return ("You added the " + itemToGet + " to your inventory");
    }

    /**
     * Method that executes the inv/inventory command
     *
     * @param tokenisedPlayerMessage player message
     * @return a list of what's in the player's inventory
     */
    public String handleInv(List<String> tokenisedPlayerMessage) {
        if (player.getPlayerInventory().isEmpty()) {
            return "Nothing to see here! Your inventory is empty";
        }

        if (detectExtraEntities(tokenisedPlayerMessage, "inv") || detectExtraEntities(tokenisedPlayerMessage, "inventory")) {
            throw new IllegalArgumentException("Error: extraneous entities detected in command!");
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("Showing ").append(player.getName()).append("'s current inventory:\n");

            player.getPlayerInventory().forEach((key, value) -> {
                builder.append(key);
                builder.append(", ");
                builder.append(value.getDescription());
                builder.append("\n");
            });
            return builder.toString();
        }
    }

    /**
     * Method to handle drop command
     *
     * @param tokenisedPlayerMessage player message
     * @return confirmation message to the player stating what item they dropped
     */
    public String handleDrop(List<String> tokenisedPlayerMessage) {
        if (player.getPlayerInventory().isEmpty()) {
            return "You have nothing to drop";
        }
        GameEntityLocation location = gameLocations.get(player.getLocation());
        String itemToDrop = findItemToDrop(tokenisedPlayerMessage);
        location.addArtefact(player.getItemFromInventory(itemToDrop));
        player.removeItemFromInventory(itemToDrop);

        return (itemToDrop + " was dropped somewhere in the " + player.getLocation());
    }

    /**
     * method for handling the goto command
     * @param tokenisedPlayerMessage player message
     * @return confirmation message telling the player where they have now travelled to
     */
    public String handleGoto(List<String> tokenisedPlayerMessage) {
        GameEntityLocation location = gameLocations.get(player.getLocation());
        String path = findPathToGoTo(tokenisedPlayerMessage, location);
        player.setLocation(path);
        location.getCharacters().remove(player.getName());
        GameEntityLocation newLocation = gameLocations.get(player.getLocation());
        newLocation.addCharacter(player);
        return ("You have travelled to: " + player.getLocation() + ", " + gameLocations.get(path).getDescription());
    }

    /**
     * method for handling the health command
     * @param tokenisedPlayerMessage player message
     * @return confirmation message telling the player how much health they have left
     */
    public String handleHealth(List<String> tokenisedPlayerMessage) {
        if (detectExtraEntities(tokenisedPlayerMessage, "health")) {
            throw new IllegalArgumentException("Error: extraneous entities detected in command!");
        }
        return "You have " + player.getHealth() + " health points remaining";
    }

    // Helper methods for handling commands

    /**
     * used in the look command to get a list of everything that should be in the player view
     * @param currentLocation location the player's in
     * @return StringBuilder containing everything that the player can see
     */
    public StringBuilder getEntitiesInView(GameEntityLocation currentLocation) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("You are in a: ").append(currentLocation.getName()).append(", ").append(currentLocation.getDescription()).append("\n");
        if (!currentLocation.getArtefacts().isEmpty()) {
            stringBuilder.append("You see items:\n");
            stringBuilder.append(currentLocation.entityToString(currentLocation.getArtefacts()));
        }
        if (!currentLocation.getFurniture().isEmpty()) {
            stringBuilder.append("You see furniture:\n");
            stringBuilder.append(currentLocation.entityToString(currentLocation.getFurniture()));
        }
        //remove current player from characters:
        currentLocation.getCharacters().remove(player.getName());
        if (!currentLocation.getCharacters().isEmpty()) {
            stringBuilder.append("You see characters:\n");
            stringBuilder.append(currentLocation.entityToString(currentLocation.getCharacters()));
        }
        //add player back in:
        currentLocation.addCharacter(player);
        if (!currentLocation.getPaths().isEmpty()) {
            stringBuilder.append(getPathList(currentLocation));
        }
        return stringBuilder;
    }

    /**
     * Method which detects extraneous entities in player command to check validity
     * @param tokenisedPlayerMessage player message
     * @param command command to skip over
     * @return true if extraneous entities present in player command
     */
    public boolean detectExtraEntities(List<String> tokenisedPlayerMessage, String command) {
        for (String string : tokenisedPlayerMessage) {
            if (!string.equals(command)) {
                if (allEntities.contains(string)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Used in look command to get the paths that the player can go to
     *
     * @param location Player's current location
     * @return list of paths available to the player
     */
    public String getPathList(GameEntityLocation location) {
        StringBuilder builder = new StringBuilder();
        builder.append("The paths available to you are:\n");
        for (String path : location.getPaths()) {
            GameEntityLocation pathLocation = gameLocations.get(path);
            builder.append(pathLocation.getName()).append(", ").append(pathLocation.getDescription());
            builder.append("\n");
        }
        return builder.toString();
    }

    /**
     * Searches location for item player wants to add to inventory
     * @param tokenisedPlayerMessage player message split into individual words
     * @param location           current location the player's in
     * @return item that the player wants to get
     */
    public String findItemToGet(List<String> tokenisedPlayerMessage, GameEntityLocation location) {
        int count = 0;
        String itemToGet = "";
        for (String item : tokenisedPlayerMessage) {
            if (location.getArtefacts().containsKey(item) && !itemToGet.equals(item)) {
                count++;
                itemToGet = item;
            }
            if (!location.getArtefacts().containsKey(item) && allEntities.contains(item)) {
                throw new IllegalArgumentException("There isn't a " + item + " here!");
            }
        }
        if (count > 1) {
            throw new IllegalArgumentException("Slow down, " + player.getName() + " can only get one item at a time!");
        }
        if (count == 0) {
            throw new IllegalArgumentException("Oops! That item doesn't exist here!");
        }
        return itemToGet;
    }

    /**
     * Searches player inventory for the item to drop
     *
     * @param tokenisedPlayerMessage player message split up into tokens
     * @return name of artefact to drop
     */
    public String findItemToDrop(List<String> tokenisedPlayerMessage) {
        String item = "";
        int count = 0;
        for (String string : tokenisedPlayerMessage) {
            if (player.checkInventory(string)) {
                item = player.getPlayerInventory().get(string).getName();
                count++;
            }
            if (!player.checkInventory(string) && allEntities.contains(string)) {
                throw new IllegalArgumentException(player.getName() + " can't find the " + item + "!");
            }
        }
        if (count > 1) {
            throw new IllegalArgumentException("Let's be careful and place down items one at a time!");
        }
        if (count == 0) {
            throw new IllegalArgumentException(player.getName() + " can't seem to find that in their inventory");
        }
        return item;
    }

    /**
     * this function gets the path the player wants to go to from their input
     *
     * @param tokenisedPlayerMessage player command tokenised
     * @param location           location the player's in
     * @return path to go to
     */
    public String findPathToGoTo(List<String> tokenisedPlayerMessage, GameEntityLocation location) {
        String path = "";
        int count = 0;
        for (String string : tokenisedPlayerMessage) {
            if (location.getPaths().contains(string.toLowerCase())) {
                path = string.toLowerCase();
                count++;
                if (count > 1) {
                    throw new IllegalArgumentException("You can't go to more than one place at once!");
                }
            }
            if (!location.getPaths().contains(string) && allEntities.contains(string)) {
                throw new IllegalArgumentException("You can't use a " + string + " to travel from here!");
            }
        }
        if (count == 0) {
            throw new IllegalArgumentException("You can't go there from this location\n" + getPathList(location));
        }
        return path;
    }

    /**
     * checks for a basic command
     *
     * @param tokenisedPlayerMessage player message split into individual words
     * @return the basic command to be executed
     */
    public String findBasicCommandInPlayerMessage(List<String> tokenisedPlayerMessage) {
        int matches = 0;
        String keyword = "";
        for (String commandWord : tokenisedPlayerMessage) {
            if (basicCommands.contains(commandWord) && !keyword.equals(commandWord)) {
                keyword = commandWord.toLowerCase();
                matches++;
            }
            if (matches > 1) {
                throw new IllegalArgumentException("Pick one command, it's hard to do two things at once :(");
            }
        }
        return keyword;
    }

}
