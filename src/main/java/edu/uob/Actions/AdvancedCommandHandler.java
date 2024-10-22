package edu.uob.Actions;

import edu.uob.Entities.GameEntityLocation;
import edu.uob.Entities.Player;

import java.util.*;

public class AdvancedCommandHandler extends GameCommandHandler{
    private final Set<String> allKeyPhrases;
    private GameEntityLocation storeroom;

    public AdvancedCommandHandler(HashMap<String, GameEntityLocation> gameLocations, HashMap<String, HashSet<GameAction>> gameActions, List<String> allEntities, HashMap<String, Player> players) {
        super(gameLocations, gameActions, allEntities, players);
        allKeyPhrases = new HashSet<>();
        gameActions.forEach((key, value) -> allKeyPhrases.add(key));
        storeroom = gameLocations.get("storeroom");
    }

    public String handleAdvancedCommand(SortedSet<String> keyPhrases, List<String> tokenisedPlayerMessage, Player player){
        Set<GameAction> doableActions = getDoableActions(keyPhrases, tokenisedPlayerMessage, player);
        return executeAction(doableActions.iterator().next(), player);
    }

    private String executeAction(GameAction action, Player player) {
        GameEntityLocation currentLocation = gameLocations.get(player.getLocation());
        produceEntities(currentLocation, action.getProducedEntities(), player);
        consumeEntities(currentLocation, action.getConsumedEntities(), player);
        return action.getNarration();
    }

    private void produceEntities(GameEntityLocation currentLocation, Set<String> producedEntities, Player player){
        for (String entity : producedEntities) {
            if (entity.equals("health")) { player.increaseHealth(); }

            else if (player.checkInventory(entity)) {
                currentLocation.addArtefact(player.getItemFromInventory(entity));
                player.getPlayerInventory().remove(entity);
            }

            else { processEntityTransfer(currentLocation, entity); }
        }
    }

    private void consumeEntities(GameEntityLocation currentLocation, Set<String> consumedEntities, Player player){
        for (String entity : consumedEntities) {
            if (gameLocations.containsKey(entity) && !currentLocation.getName().equals(entity)) {
                if (currentLocation.getPaths().contains(entity)) {
                    currentLocation.getPaths().remove(entity);
                } else throw new IllegalArgumentException("Location to consume doesn't exist!");
            }

            if (entity.equals("health")) {
                consumePlayerHealth(currentLocation, player);
            }

            else {
                GameEntityLocation sourceLocation = findEntityLocation(entity);
                moveEntityToStoreroom(sourceLocation, entity, player);
            }
        }
    }

    //helper functions for consuming / producing entities

    public GameEntityLocation findEntityLocation(String entity) {
        GameEntityLocation location;
        for (Map.Entry<String, GameEntityLocation> locationEntry : gameLocations.entrySet()) {
            location = locationEntry.getValue();
            if (location.getEntitiesList().contains(entity)) { return location; }
        }
        return null;
    }

    private void consumePlayerHealth(GameEntityLocation currentLocation, Player player){
        if (player.getHealth() == 1) {
            player.getPlayerInventory().forEach((key, value) -> currentLocation.addArtefact(value));
            player.resetPlayer();
        } else { player.reduceHealth(); }
    }

    private void processEntityTransfer(GameEntityLocation currentLocation, String entity){
        GameEntityLocation sourceLocation = findEntityLocation(entity);

        if (sourceLocation == null ) { throw new IllegalArgumentException("Entity to produce cannot be found!"); }

        else {
            transferEntity(sourceLocation.getArtefacts(), currentLocation.getArtefacts(), entity);
            transferEntity(sourceLocation.getCharacters(), currentLocation.getCharacters(), entity);
            transferEntity(sourceLocation.getFurniture(), currentLocation.getFurniture(), entity);
            if (gameLocations.containsKey(entity) && !currentLocation.getName().equals(entity)) { currentLocation.addPath(entity); }
        }
    }

    private void moveEntityToStoreroom(GameEntityLocation sourceLocation, String entity, Player player){
        if (player.checkInventory(entity)) {
            transferEntity(player.getPlayerInventory(), storeroom.getArtefacts(), entity);
        }

        else if (sourceLocation == null) { throw new IllegalArgumentException("Cannot locate entity to be consumed!"); }

        else {
            transferEntity(sourceLocation.getCharacters(), storeroom.getCharacters(), entity);
            transferEntity(sourceLocation.getFurniture(), storeroom.getFurniture(), entity);
            transferEntity(sourceLocation.getArtefacts(), storeroom.getArtefacts(), entity);
        }
    }

    private <T> void transferEntity(Map<String, T> source, Map<String, T> destination, String entity) {
        T item = source.get(entity);
        if (item != null){
            source.remove(entity);
            destination.put(entity, item);
        }
    }

    //helper functions for parsing player command for the correct action

    /**
     *
     * @param keyPhrases keyphrases found in the player command
     * @param tokenisedPlayerMessage
     * @param player
     * @return
     */
    private Set<GameAction> getDoableActions(SortedSet<String> keyPhrases, List<String> tokenisedPlayerMessage, Player player) {
        Set<GameAction> doableActions = new HashSet<>();

        //go through all potential commands and get the doable action
        for (String keyPhrase : keyPhrases) {
            HashSet<GameAction> actions = gameActions.get(keyPhrase);
            validateActionSet(actions, doableActions, tokenisedPlayerMessage, player);
        }

        if (doableActions.isEmpty()) {
            throw new IllegalArgumentException("Error: Invalid command");
        } else if (doableActions.size() > 1 ) { throw new IllegalArgumentException("Oh no! " + player.getName() +
                " doesn't know what to do!\nWhat action would you like to perform?"); }

        return doableActions;
    }

    private void validateActionSet(HashSet<GameAction> actions, Set<GameAction> doableActions, List<String> tokenisedPlayerMessage, Player player){
        for (GameAction action : actions) {
            if (checkActionIsDoable(action, tokenisedPlayerMessage, player)) {
                doableActions.add(action);
            }
        }
    }

    private boolean checkActionIsDoable(GameAction action, List<String> tokenisedPlayerMessage, Player player) {
        Set<String> actionSubjects = action.getSubjects();
        List<String> locationEntities = new ArrayList<>(gameLocations.get(player.getLocation()).getEntitiesList());
        player.getPlayerInventory().forEach((key, value) -> locationEntities.add(key));

        for (String subject : actionSubjects) {
            if (!locationEntities.contains(subject)) {
                return false;
            }
        }
        return (validatePlayerMessage(action, tokenisedPlayerMessage) && checkEntitiesNotInPlayerInventories(action, player));
    }

    private boolean validatePlayerMessage(GameAction action, List<String> tokenisedPlayerMessage){
        boolean match = false;
        for (String token : tokenisedPlayerMessage) {
            if (action.getSubjects().contains(token)) { match = true; }
            if (!action.getSubjects().contains(token) && allEntities.contains(token)) { return false; }
        }
        return match;
    }

    private boolean checkEntitiesNotInPlayerInventories(GameAction action, Player player){
        Set<String> actionEntities = new HashSet<>(action.getProducedEntities());
        actionEntities.addAll(action.getConsumedEntities());
        for (String entity : actionEntities) {
            for (Player gamePlayers : players.values()) {
                if (gamePlayers.checkInventory(entity) && !gamePlayers.getName().equals(player.getName())) {
                    throw new IllegalArgumentException("An item needed for this action is in another player's inventory!");
                }
            }
        }
        return true;
    }

    /**
     * Parses player command and returns a set of all key phrases in the command
     * @param tokenisedPlayerMessage Tokenised list of player message, split by spaces
     * @return A set of all potential commands found in the player message
     * The set is ordered from longest command first to avoid false positives
     * e.g. detecting the "cut" when the player meant "cut down" and trying to execute that command
     */
    public SortedSet<String> findTriggersInPlayerMessage(List<String> tokenisedPlayerMessage){
        int maxPhraseLength = getPhraseLength();
        SortedSet<String> foundKeyPhrases = new TreeSet<>( Collections.reverseOrder());

        for (int i = 0; i < tokenisedPlayerMessage.size(); i++) {
            StringBuilder builder = new StringBuilder();

            for (int j = i; j < i + maxPhraseLength && j < tokenisedPlayerMessage.size(); j++) {
                if (j > i) { builder.append(" "); }
                builder.append(tokenisedPlayerMessage.get(j));
                String currentPhrase = builder.toString();

                if (allKeyPhrases.contains(currentPhrase)) {
                    foundKeyPhrases.add(currentPhrase);
                }
            }
        }
        return foundKeyPhrases;
    }

    /**
     * This method goes through the list of trigger phrases and finds the one that has the most words in
     * This is then used to create a "sliding window" to find triggers in the tokenised player command
     * @return max phrase length
     */
    private int getPhraseLength() {
        return allKeyPhrases.stream().mapToInt(
                phrase -> phrase.split("\\s+").length)
               .max().orElse(1);
    }

}
