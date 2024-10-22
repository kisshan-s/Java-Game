package edu.uob.Entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GameEntityLocation extends GameEntity {
    private List<String> locationPaths;
    private HashMap<String, GameEntityArtefact> artefactHashMap;
    private HashMap<String, GameEntityFurniture> furnitureHashMap;
    private HashMap<String, GameEntityCharacter> characterHashMap;
    public GameEntityLocation(String name, String description) {
        super(name, description);
        artefactHashMap = new HashMap<>();
        furnitureHashMap = new HashMap<>();
        characterHashMap = new HashMap<>();
        locationPaths = new ArrayList<>();
    }

    /**
     * getter methods
     * @return ArrayList of relevant GameEntity
     */
    public HashMap<String, GameEntityArtefact> getArtefacts() { return artefactHashMap; }

    public HashMap<String, GameEntityFurniture> getFurniture() {
        return furnitureHashMap;
    }

    public HashMap<String, GameEntityCharacter> getCharacters() {
        return characterHashMap;
    }

    public ArrayList<String> getPaths() { return (ArrayList<String>) locationPaths; }

    public ArrayList<String> getEntitiesList() {
        ArrayList<String> entities = new ArrayList<>();
        entities.add(getName());
        artefactHashMap.forEach((key, value) -> entities.add(key));
        characterHashMap.forEach((key, value) -> entities.add(key));
        furnitureHashMap.forEach((key, value) -> entities.add(key));
        return entities;
    }

    /**
     * add artefact to the hashmap
     * @param artefact The artefact that belongs to the location
     */
    public void addArtefact (GameEntityArtefact artefact){ this.artefactHashMap.put(artefact.getName(), artefact); }


    /**
     * add furniture to hashmap
     * @param furniture Furniture present in the location
     */
    public void addFurniture (GameEntityFurniture furniture){ this.furnitureHashMap.put(furniture.getName(), furniture); }

    /**
     * add character to hashmap
     * @param character Character present in the location
     */
    public void addCharacter (GameEntityCharacter character){
        if (!getCharacters().containsKey(character.getName())) {
            this.characterHashMap.put(character.getName(), character);
        }
    }

    /**
     * add path to list
     * @param path Path/location ID that player can go to from this location
     */
    public void addPath (String path) { locationPaths.add(path); }

    /**
     * method used for the 'look' command,
     * @return a string which lists all entities within the hashmap
     */

    //getting all artefacts
    public String entityToString(HashMap<?, ? extends GameEntity> entityHashMap) {
        if (entityHashMap.isEmpty()) { return ""; }

        else {
            StringBuilder builder = new StringBuilder();

            entityHashMap.forEach((key, value) -> {
                builder.append(key);
                builder.append(", ");
                builder.append(value.getDescription());
                builder.append("\n");
            });
            return builder.toString();
        }
    }

    }
