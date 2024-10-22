package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.alexmerz.graphviz.*;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;
import edu.uob.Actions.GameAction;
import edu.uob.Actions.GameCommandHandler;
import edu.uob.Entities.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class GameServer {

    private static final char END_OF_TRANSMISSION = 4;

    public static void main(String[] args) throws IOException {
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
        GameServer server = new GameServer(entitiesFile, actionsFile);
        server.blockingListenOn(8888);
    }
    public final HashMap<String, GameEntityLocation> gameLocations = new HashMap<>();
    private final List<String> reservedWords = Arrays.asList("inv", "goto", "inventory", "drop", "look");
    public final HashMap<String, HashSet<GameAction>> gameActionMap = new HashMap<>();
    public HashMap<String, Player> players = new HashMap<>();
    private String startingLocation = null;
    GameCommandHandler commandHandler;
    List<String> allEntities;

    /**
    * Instantiates a new server instance, specifying a game with some configuration files
    * @param entitiesFile The game configuration file containing all game entities
    * @param actionsFile The game configuration file containing all game actions
    */
    public GameServer(File entitiesFile, File actionsFile) {
        try {
            getEntitiesFromFile(entitiesFile);
        } catch (IOException | IllegalArgumentException | ParseException e){
            throw new RuntimeException(e);
        }
        try {
            getActionsFromFile(actionsFile);
        } catch (IOException | SAXException | ParserConfigurationException e){
            throw new RuntimeException(e);
        }
        allEntities = new ArrayList<>();
        commandHandler = new GameCommandHandler(gameLocations, gameActionMap, allEntities, players);
        gameLocations.forEach((key, value) -> allEntities.addAll(value.getEntitiesList()));
    }

    /**
     * This method handles all incoming game commands and carries out the corresponding actions.
     * @param command The incoming command to be processed
     */
    public String handleCommand(String command) {
        if (!command.contains(":")) {return "Error: Invalid player name!";}
        //server logic here
        String[] inputParts = command.split(":", 2);
        String playerName = inputParts[0].trim();
        String playerCommand = inputParts[1].trim().toLowerCase();

        try {
            Player player = getPlayer(playerName);
            return commandHandler.handleCommand(playerCommand, player);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /**
     * Sets up a document builder to read the file and get a list of all possible actions in the game.
     * The actions are then passed to createGameAction so that all relevant info can be extracted
     *
     * @param actionsFile the XML file containing actions available in the game
     * @throws ParserConfigurationException thrown if document builder could not be created
     * @throws IOException                  thrown if any IO errors occur when attempting to read actions file
     * @throws SAXException                 thrown if any parse errors happen when attempting to read actions file
     */
    public void getActionsFromFile(File actionsFile) throws ParserConfigurationException, IOException, SAXException {
        // setting up the document builder to obtain a list of all actions contained in the .xml file
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(actionsFile);
        document.getDocumentElement().normalize();
        Element root = document.getDocumentElement();
        List<Element> actions = getElements(root.getChildNodes());
        //go through each action and for each action, extract all relevant GameAction information
        for (Element action : actions) {
            createGameAction(action);
        }
    }

    /**
     * This method essentially converts a NodeList to an ElementList so that the sub elements of each action can be accessed
     *
     * @param nodes list of "nodes" - all actions in the game
     * @return list of elements (Actions) that can be parsed in more detail to obtain subjects/consumed/produced/narration
     */
    public List<Element> getElements(NodeList nodes) {
        List<Element> elementList = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            //check that this is an element node - ignores text or comments
            if (nodes.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                elementList.add((Element) nodes.item(i));
            }
        }
        return elementList;
    }

    /**
     * This method takes in an action and creates a new GameAction class for that action
     * The method populates the class with keyphrases / triggers, subjects, consumed entities, produced entities, and the narration string
     * The new GameAction instance is then added to the gameActionMap via the computeIfAbsent lambda method, which prevents duplication
     *
     * @param action - everything that comprises an `<action> </action>` pair in the .xml file
     */
    public void createGameAction(Element action) {
        //get the trigger element from the action element
        Element trigger = (Element) action.getElementsByTagName("triggers").item(0);
        List<Element> triggers = getElements(trigger.getChildNodes());

        Set<String> keyPhrases = new HashSet<>();
        for (Element keyphrase : triggers) {
            keyPhrases.add(keyphrase.getTextContent().toLowerCase());
        }

        Set<String> subjects = extractSetFromElement(action, "subjects");
        Set<String> consumedEntities = extractSetFromElement(action, "consumed");
        Set<String> producedEntities = extractSetFromElement(action, "produced");
        String narration = action.getElementsByTagName("narration").item(0).getTextContent();

        GameAction gameAction = new GameAction(keyPhrases, subjects, consumedEntities, producedEntities, narration);
        for (String phrase : keyPhrases) {
            gameActionMap.computeIfAbsent(phrase, hashSetActions -> new HashSet<>()).add(gameAction);
        }
    }

    /**
     * This method is used to obtain the sets of subjects, consumed entities, and produced entities contained in an action
     *
     * @param parentElement the overall GameAction
     * @param elementTag    name of the element (e.g. <subjects></subjects> ) to search in
     * @return a set of the items within the element (e.g. list of consumed entities)
     */
    private Set<String> extractSetFromElement(Element parentElement, String elementTag) {
        Set<String> elementSet = new HashSet<>();
        if (parentElement != null) {
            NodeList nodes = parentElement.getElementsByTagName(elementTag).item(0).getChildNodes();
            List<Element> elements = getElements(nodes);
            for (Element element : elements) {
                elementSet.add(element.getTextContent().toLowerCase());
            }
        }
        return elementSet;
    }

    /**
     * loading entities
     * sets up a dot parser that converts the DOT file into a graph
     * the graph can be explored to get locations, location details, and paths
     * @param entitiesFile The DOT file containing all the game entities
     * @throws IOException              For issues with file reading
     * @throws ParseException           For incorrect DOT format
     * @throws IllegalArgumentException For invalid entity names
     */
    public void getEntitiesFromFile(File entitiesFile) throws IOException, ParseException, IllegalArgumentException {
        FileReader reader = new FileReader(entitiesFile);
        Parser parser = new Parser();
        parser.parse(reader); //assert that you can parse the file
        Graph fileGraph = parser.getGraphs().get(0);
        ArrayList<Graph> sections = fileGraph.getSubgraphs();
        //get locations
        ArrayList<Graph> locations = sections.get(0).getSubgraphs();

        //for loop for each location
        boolean firstLoop = true;
        for (Graph locationGraph : locations) {
            addLocationEntityToList(locationGraph, firstLoop);
            firstLoop = false;
        }
        //get path info
        ArrayList<Edge> paths = sections.get(1).getEdges();
        processPaths(paths);
    }

    /**
     * set up each location in its own class
     * takes in a locationGraph and sets up a GameEntityLocation instance with all the relevant information
     * this GEL is then added to the hashmap of locations stored in the GameServer class
     * @param locationGraph the specific location subgraph e.g. cabin that has entities inside it;
     */
    public void addLocationEntityToList(Graph locationGraph, Boolean firstLoop) {
        String locationName;
        String locationDescription;
        Node locationDetails = locationGraph.getNodes(false).get(0);
        //extract location name and description from node
        locationName = locationDetails.getId().getId().toLowerCase();
        if (checkIfReservedWord(locationName)) {
            throw new IllegalArgumentException(locationName + " is a reserved word in this game, please use a different name.");
        }
        locationDescription = locationDetails.getAttribute("description").toLowerCase();
        GameEntityLocation location = new GameEntityLocation(locationName, locationDescription);
        //store the starting locationxx
        if (firstLoop) {
            startingLocation = locationName;
        }
        //check if location has any subEntities
        if (!locationGraph.getSubgraphs().isEmpty()) {
            // add subEntities to the location class
            processSubGraphs(locationGraph, location);
        }
        gameLocations.put(locationName, location);
    }

    /**
     * processSubGraphs
     * process location sub-entities and add them to the location class
     * @param locationGraph the specific location subgraph e.g. cabin that has entities inside it
     * @param location      the instance of GameEntityLocation that will be stored in the list
     */
    private void processSubGraphs(Graph locationGraph, GameEntityLocation location) {
        for (Graph subGraph : locationGraph.getSubgraphs()) {
            String entityType = subGraph.getId().getId(); //get the entity type
            for (Node node : subGraph.getNodes(false)) {
                String nodeName = node.getId().getId().toLowerCase();
                String nodeDescription = node.getAttribute("description").toLowerCase();

                if (checkIfReservedWord(nodeName)) {
                    throw new IllegalArgumentException(nodeName + " is a reserved word in this game, please use a different name.");
                }

                switch (entityType) {
                    case "artefacts":
                        GameEntityArtefact locationArtefact = new GameEntityArtefact(nodeName, nodeDescription);
                        location.addArtefact(locationArtefact);
                        break;
                    case "furniture":
                        GameEntityFurniture locationFurniture = new GameEntityFurniture(nodeName, nodeDescription);
                        location.addFurniture(locationFurniture);
                        break;
                    case "characters":
                        GameEntityCharacter locationCharacter = new GameEntityCharacter(nodeName, nodeDescription);
                        location.addCharacter(locationCharacter);
                        break;
                }
            }
        }
    }

    /**
     * This method validates each path by checking both source and destination exist and adds them to each location
     * @param paths - found in the second subgraph in the entities file
     */
    private void processPaths(ArrayList<Edge> paths) {
        for (Edge path : paths) {
            String pathFrom = path.getSource().getNode().getId().getId().toLowerCase();
            String pathTo = path.getTarget().getNode().getId().getId().toLowerCase();
            if (gameLocations.containsKey(pathFrom) && gameLocations.containsKey(pathTo)) {
                gameLocations.get(pathFrom).addPath(pathTo);
            } else throw new IllegalArgumentException();
        }
    }

    /**
     * method to check all entity names are valid and not taken by the server
     * @param word The string being checked
     * @return true if the reservedWords list contains the @param word
     */
    private boolean checkIfReservedWord(String word) {
        return (reservedWords.contains(word.toLowerCase()));
    }

    /**
     * This method is used to obtain the Player object of the client
     * @param playerName the player's name as specified in the command
     * @return the Player object mapped to the player's name after checking the name is valid
     */
    public Player getPlayer(String playerName) {
        String regex = "[a-zA-Z\\s '-]+$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(playerName);

        if (checkIfReservedWord(playerName)) {
            throw new IllegalArgumentException("That player name is unavailable as it is a reserved word\n" +
                    "Please select a new name.");
        }

        if (matcher.matches()) {
            assignPlayer(playerName);
            return players.get(playerName);
        } else throw new IllegalArgumentException("Error: " + playerName + " is an invalid player name!");
    }

    /**
     * This method first checks if the player is a new one.
     * If so, it creates a new Player, placing them in the starting location and adding them to the hashmap of players
     *
     * @param playerName player name the client is using
     */
    public void assignPlayer(String playerName) {
        if (!players.containsKey(playerName)) {
            Player player = new Player(playerName, "A friendly player", startingLocation);
            players.put(playerName, player);
            player.setLocation(startingLocation);
            gameLocations.get(startingLocation).addCharacter(player);
        }
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Starts a *blocking* socket server listening for new connections.
    *
    * @param portNumber The port to listen on.
    * @throws IOException If any IO related operation fails.
    */
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            }
        }
    }

    /**
    * Handles an incoming connection from the socket server.
    * @param serverSocket The client socket to read/write from.
    * @throws IOException If any IO related operation fails.
    */
    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            System.out.println("Connection established");
            String incomingCommand = reader.readLine();
            if(incomingCommand != null) {
                System.out.println("Received message from " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
