package edu.uob;

import edu.uob.Entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;
import com.alexmerz.graphviz.objects.Edge;

import static org.junit.jupiter.api.Assertions.*;

final class EntitiesFileTests {

    File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
    File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();
    GameServer testServer;

  // Test to make sure that the basic entities file is readable
  @Test
  void testBasicEntitiesFileIsReadable() {

      try {
          Parser parser = new Parser();
          FileReader reader = new FileReader("config" + File.separator + "basic-entities.dot");
          parser.parse(reader);
          Graph wholeDocument = parser.getGraphs().get(0);
          ArrayList<Graph> sections = wholeDocument.getSubgraphs();

          // The locations will always be in the first subgraph
          ArrayList<Graph> locations = sections.get(0).getSubgraphs();
          Graph firstLocation = locations.get(0);
          Node locationDetails = firstLocation.getNodes(false).get(0);
//          System.out.println(locationDetails.getId().getId());
//          System.out.println(locationDetails.getAttribute("description"));
          // Yes, you do need to get the ID twice !
          String locationName = locationDetails.getId().getId();
          assertEquals("cabin", locationName, "First location should have been 'cabin'");

          // The paths will always be in the second subgraph
          ArrayList<Edge> paths = sections.get(1).getEdges();
          Edge firstPath = paths.get(0);
          Node fromLocation = firstPath.getSource().getNode();
          String fromName = fromLocation.getId().getId();
          Node toLocation = firstPath.getTarget().getNode();
          String toName = toLocation.getId().getId();
          assertEquals("cabin", fromName, "First path should have been from 'cabin'");
          assertEquals("forest", toName, "First path should have been to 'forest'");

      } catch (FileNotFoundException fnfe) {
          fail("FileNotFoundException was thrown when attempting to read basic entities file");
      } catch (ParseException pe) {
          fail("ParseException was thrown when attempting to read basic entities file");
      }
  }
  
  @BeforeEach
  void setUpServer(){
        testServer = new GameServer(entitiesFile, actionsFile);
  }
  @Test
    void testEntitiesAddedToList(){
      assertEquals(4, testServer.gameLocations.size());
      assertInstanceOf(GameEntityLocation.class, testServer.gameLocations.get("cabin"));
      assertEquals("storeroom", testServer.gameLocations.get("storeroom").getName());
      assertFalse(testServer.gameLocations.isEmpty());
//      System.out.println(testServer.gameLocations.get("cabin").entityToString((testServer.gameLocations.get("cabin").getCharacters())));
//      System.out.println(testServer.gameLocations.get("cabin").entityToString((testServer.gameLocations.get("cabin").getArtefacts())));
//      System.out.println(testServer.gameLocations.get("cabin").entityToString((testServer.gameLocations.get("cabin").getFurniture())));
//      System.out.println(testServer.gameLocations.get("cabin").pathsToString());


  }
  
  @Test
    void testSubArtefactsAdded(){
      assertTrue(testServer.gameLocations.containsKey("cabin"));
      GameEntityLocation cabin = testServer.gameLocations.get("cabin");
      assertTrue(cabin.getArtefacts().containsKey("axe"));
      assertEquals("axe", cabin.getArtefacts().get("axe").getName());
      assertEquals("a razor sharp axe", cabin.getArtefacts().get("axe").getDescription());
      assertTrue(cabin.getArtefacts().containsKey("potion"));
      assertEquals("magic potion", cabin.getArtefacts().get("potion").getDescription());
  }

  @Test
    void testSubFurnitureAdded(){
      assertTrue(testServer.gameLocations.containsKey("forest"));
      GameEntityLocation forest = testServer.gameLocations.get("forest");
      GameEntityFurniture furniture = forest.getFurniture().get("tree");
      assertEquals("tree", furniture.getName());
      assertEquals("a big tree", furniture.getDescription());
      assertInstanceOf(GameEntityFurniture.class, forest.getFurniture().get("tree"));
  }

  @Test
    void testSubCharactersAdded(){
      assertTrue(testServer.gameLocations.containsKey("cellar"));
      GameEntityLocation cellar = testServer.gameLocations.get("cellar");
      assertInstanceOf(GameEntityCharacter.class, cellar.getCharacters().get("elf"));
      GameEntityCharacter elf = cellar.getCharacters().get("elf");
      assertEquals("elf", elf.getName());
      assertEquals("angry elf", elf.getDescription());
  }

  @Test
    void testPathsAdded(){
      testServer.gameLocations.forEach((key, value) -> {
          assert key.equalsIgnoreCase(value.getName());
          if (!value.getPaths().isEmpty()){
              for (String path : value.getPaths()) {
                  assertInstanceOf(GameEntityLocation.class, testServer.gameLocations.get(path));
              }
          }
      });
      System.out.println(testServer.gameLocations.get("cabin").getPaths());
  }

  @Test
    void testPLayersAdded(){
      testServer.handleCommand("simon : look");
//      System.out.println(testServer.gameLocations.get("cabin").getCharacters());
      assertInstanceOf(Player.class, testServer.gameLocations.get("cabin").getCharacters().get("simon"));
      //also assert instance of character to check polymorphism
      assertInstanceOf(GameEntityCharacter.class, testServer.gameLocations.get("cabin").getCharacters().get("simon"));
      System.out.println(testServer.players.get("simon").getLocation());
      //adding another player to the same location
      testServer.handleCommand("mia : look");
      assertInstanceOf(Player.class, testServer.gameLocations.get(testServer.players.get("mia").getLocation()).getCharacters().get("mia"));
//      System.out.println("characters in cabin after mia added: " + testServer.gameLocations.get("cabin").getCharacters());
//      System.out.println("testserver players after mia added: " + testServer.players);

      //checking that both players are present:
      assertTrue(testServer.gameLocations.get("cabin").getCharacters().containsKey("simon"));
      assertTrue(testServer.gameLocations.get("cabin").getCharacters().containsKey("mia"));
      assert (testServer.gameLocations.get("cabin").getCharacters().containsKey("mia"));
      assertEquals(2, testServer.players.size());
      assertFalse(testServer.gameLocations.get("forest").getCharacters().containsKey("mia"));
  }

  @Test
    void testEntitiesToList(){
      testServer.handleCommand("simon: look");

      ArrayList<String> entities = testServer.gameLocations.get("cabin").getEntitiesList();
      assertTrue(entities.contains("axe"));
      testServer.handleCommand("mia: get axe");
      entities = testServer.gameLocations.get("cabin").getEntitiesList();
      //check that we can't see into player inventories
      assertFalse(entities.contains("axe"));

      testServer.handleCommand("simon: get potion");

      assertTrue(entities.contains("potion"));
  }

}
