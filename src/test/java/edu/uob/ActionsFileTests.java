package edu.uob;

import edu.uob.Actions.AdvancedCommandHandler;
import edu.uob.Actions.GameAction;
import edu.uob.Entities.GameEntityLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.*;
import java.io.IOException;
import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.*;

final class ActionsFileTests {
    File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();
    File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
    File extEntities = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
    File extActions = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
    File customActions = Paths.get("config" + File.separator + "custom-actions.xml").toAbsolutePath().toFile();
    File customEntities = Paths.get("config" + File.separator + "custom-entities.dot").toAbsolutePath().toFile();

    GameServer testServer;
    GameServer extServer;
    GameServer complexServer;

  // Test to make sure that the basic actions file is readable
  @Test
  void testBasicActionsFileIsReadable() {
      try {
          DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
          Document document = builder.parse("config" + File.separator + "basic-actions.xml");
          Element root = document.getDocumentElement();
          NodeList actions = root.getChildNodes();
          // Get the first action (only the odd items are actually actions - 1, 3, 5 etc.)
          Element firstAction = (Element)actions.item(1);
          Element triggers = (Element)firstAction.getElementsByTagName("triggers").item(0);
          // Get the first trigger phrase
          String firstTriggerPhrase = triggers.getElementsByTagName("keyphrase").item(0).getTextContent();
          assertEquals("open", firstTriggerPhrase, "First trigger phrase was not 'open'");
      } catch(ParserConfigurationException pce) {
          fail("ParserConfigurationException was thrown when attempting to read basic actions file");
      } catch(SAXException saxe) {
          fail("SAXException was thrown when attempting to read basic actions file");
      } catch(IOException ioe) {
          fail("IOException was thrown when attempting to read basic actions file");
      }
  }

    @BeforeEach
    void setUpServer(){
      testServer = new GameServer(entitiesFile, actionsFile);
      extServer = new GameServer(extEntities, extActions);
      complexServer = new GameServer(customEntities, customActions);
  }

    @Test
    void testActionsAddedToList(){
        System.out.println("Testing actions have been added to the action map: ");
         testServer.gameActionMap.forEach((key, hashset) -> {
             System.out.println(key);
             System.out.println("hashset size: " + hashset.size());
                 assertInstanceOf(GameAction.class, hashset.iterator().next());
                 
             for (GameAction action : hashset) {
                 assert (action.getKeyPhrases().contains(key)); //assert that the key we use to access this GameAction is in the keyphrases
                 System.out.println(action.getKeyPhrases());
                 System.out.println(action.getNarration());
             }
         });
    }

    @Test
    void testLook(){
      String response = testServer.handleCommand("simon: look");
      System.out.println(response);
      assert response.contains("cabin");
      assert response.contains("potion");
      assert response.contains("a razor sharp axe");
      assert response.contains("forest");
    }

    @Test
    void testGet(){
        //test that we can't get multiple items in one command
        String response = testServer.handleCommand("simon: get axe and potion");
        assertFalse(response.contains("added"));
        //test that we can get the axe
        response = testServer.handleCommand("simon: get axe axe axe");
        assertTrue(response.contains("axe"));
        //testing that the axe is in simon's inventory
        assertEquals("axe", testServer.players.get("simon").getPlayerInventory().get("axe").getName());
        //test that the axe no longer exists in the gamelocation
        assertFalse(testServer.gameLocations.get("cabin").getArtefacts().containsKey("axe"));
        //test error thrown when trying to get the axe again
        response = testServer.handleCommand("simon: get axe");
        assertFalse(response.contains("added axe"));
        //test we can't get an item that exists in a different location
        assertFalse((testServer.handleCommand("simon: get key")).contains("added"));
        //test that other players can't get the item (testing each item and location is unique)
        assertFalse((testServer.handleCommand("mia: get axe")).contains("added"));

        //test that we don't accept extraneous entities
        testServer.handleCommand("simon: drop axe");
        response = testServer.handleCommand("simon: get axe potion");
        assertFalse(response.contains("axe"));
    }

    @Test
    void testInv(){
      String response = testServer.handleCommand("simon: inv");
      String verboseResponse = testServer.handleCommand("simon: inventory");
      //assert same response for `inv` and `inventory`
      assertEquals(response, verboseResponse);
      //assert response for empty inventory
      assert response.contains("empty");
      testServer.handleCommand("simon: get axe");
      assert testServer.handleCommand("simon: inv").contains("axe");

      response = testServer.handleCommand("simon: inv potion");
      assertFalse(response.contains("inventory"));

      response = testServer.handleCommand("simon: inv cabin");
      assertFalse(response.contains("axe"));
    }

    @Test
    void testDrop(){
      String failed = testServer.handleCommand("simon: drop axe");
        System.out.println(failed);
        assertTrue(failed.contains("nothing"));

      testServer.handleCommand("simon: get axe");
      String response = testServer.handleCommand("simon: drop axe");
        System.out.println(response);
        assertTrue(response.contains("axe"));

    }
    @Test
    void testGoto(){
      String response;
      testServer.handleCommand("simon: inv");
      testServer.handleCommand("mia: inv");
      response = testServer.handleCommand("simon: look");
      //check players can see each other
      assertTrue(response.contains("mia"));
      testServer.handleCommand("simon: goto forest");
      response = testServer.handleCommand("mia: look");
      //check that a player can't see someone in another location
      assertFalse(response.contains("simon"));

    }

    @Test
    void testHealth(){
      String response;
      testServer.handleCommand("simon: inv");
      response = testServer.handleCommand("simon: health");
      System.out.println(response);
      assertTrue(response.contains("3"));
      testServer.players.get("simon").setLocation("cellar");
    }

    @Test
    void testAdvancedCommandParser(){
      AdvancedCommandHandler handler = new AdvancedCommandHandler(testServer.gameLocations, testServer.gameActionMap, testServer.allEntities, testServer.players);
      String command = "I want to unlock the door";
      String fight = "I got SERIOUS beef w this elf let me FIGHT him";
      String split = "I would really like to cutdown that tree";
      String two = "you should cu t down that log";

      Set<String> matched = handler.findTriggersInPlayerMessage(Arrays.asList(command.split("\\s+")));
      Set<String> yes = handler.findTriggersInPlayerMessage(Arrays.asList(fight.toLowerCase().split("\\s+")));
      SortedSet<String> woah = handler.findTriggersInPlayerMessage(Arrays.asList(split.split("\\s+")));
      Set<String> wow = handler.findTriggersInPlayerMessage(Arrays.asList(two.split("\\s+")));

      assertTrue(matched.contains("unlock"));
      assertTrue(yes.contains("fight"));
      assertTrue(woah.contains("cutdown"));
      assertFalse(wow.contains("cut down"));

    }

    @Test
    void testInvalidPlayerName(){
      testServer.handleCommand("simon: look");
        System.out.println(testServer.handleCommand("s1mon: look"));
        String response = testServer.handleCommand("simon: look");

        assertFalse(response.contains("s1mon"));
    }

    @Test
    void testDoableActions(){
      testServer.handleCommand("simon: look");
      String response = testServer.handleCommand("simon: unlock trapdoor");
      assertFalse(response.contains("unlock"));
    }

    @Test
    void testItemsConsumed(){
      testServer.handleCommand("simon: get axe");
      testServer.handleCommand("simon: goto forest");
      //storeroom shouldn't contain the tree
      assertFalse(testServer.gameLocations.get("storeroom").getFurniture().containsKey("tree"));
      testServer.handleCommand("simon: chop tree");
      //check tree isn't still present in forest
      assertFalse(testServer.gameLocations.get("forest").getFurniture().containsKey("tree"));
      //check tree was moved over to the storeroom
      assertTrue(testServer.gameLocations.get("storeroom").getFurniture().containsKey("tree"));
    }

    @Test
    void testExtraEntities(){
      String response;
      testServer.handleCommand("simon: look");

      testServer.handleCommand("simon: get look axe");
      response = testServer.handleCommand("simon: inv");
      assertFalse(response.contains("axe"));

      response = testServer.handleCommand("simon: look cabin");
      assertFalse(response.contains("You are in a"));
    }

    @Test
    void testCommandOrder(){
      String response;
      testServer.handleCommand("simon: get axe");
      response = testServer.handleCommand("simon: DROP DA AXE");
      assertTrue(response.contains("dropped"));
      response = testServer.handleCommand("drop axe");
      assertFalse(response.contains("dropped"));

    }

    @Test
    void testAdvancedCommands(){
      String response;
      testServer.handleCommand("simon: get axe");
      testServer.handleCommand("simon: goto forest");
      response = testServer.handleCommand("simon: look");
      assertTrue(response.contains("tree"));

      response = testServer.handleCommand("simon: chop down that tree");
      assertTrue(response.contains("You cut down the tree with the axe"));

      response = testServer.handleCommand("simon: look");
      assertFalse(response.contains("tree"));
    }

    @Test
    void testFindEntityLocation(){
      AdvancedCommandHandler handler = new AdvancedCommandHandler(testServer.gameLocations, testServer.gameActionMap, testServer.allEntities, testServer.players);
        GameEntityLocation location = handler.findEntityLocation("axe");
        assertEquals("cabin", location.getName());

        testServer.handleCommand("simon: get potion");
        testServer.handleCommand("simon: goto forest");
        location = handler.findEntityLocation("potion");
        assertNull(location);

        testServer.handleCommand("simon: drop potion");
        location = handler.findEntityLocation("potion");
        assertEquals("forest", location.getName());
    }

    @Test
    void testComplexActions(){
      complexServer.handleCommand("tony: goto forest");
      complexServer.handleCommand("tony: get flute");
      complexServer.handleCommand("tony: goto cabin");

      //testing that locations are detected as subjects
      assertTrue(complexServer.handleCommand("tony: play flute").contains("Error"));
      complexServer.handleCommand("tony: goto forest");
      assertTrue(complexServer.handleCommand("tony: play flute").contains("You play the magic flute"));
    }

    @Test
    void testMultipleActionsAvailable(){
        complexServer.handleCommand("harry: look");
        complexServer.handleCommand("harry: goto forest");
        complexServer.handleCommand("harry: get key");
        complexServer.handleCommand("harry: get hammer");
        complexServer.handleCommand("harry: goto cabin");
        //test command is not accepted if multiple actions can be done at once
        assertFalse(complexServer.handleCommand("harry: unlock with hammer").contains("bash"));
        assertFalse(complexServer.handleCommand("harry: unlock da door the trapdoor").contains("unlock"));
        System.out.println(complexServer.handleCommand("harry: unlock da door the trapdoor"));
        //command accepted if player is more specific
        assertTrue(complexServer.handleCommand("harry: unlock da door with key").contains("unlock"));
    }

    @Test
    void testConsumingAndProducing(){
      complexServer.handleCommand("callum: look");
      complexServer.players.get("callum").setLocation("desert");
      complexServer.handleCommand("callum: get sword");
      complexServer.players.get("callum").setLocation("cellar");
      //kill elf so that it is consumed
        complexServer.handleCommand("callum: cut elf");
        //check elf is now in the storeroom
        assertTrue(complexServer.gameLocations.get("storeroom").getCharacters().containsKey("elf"));
        complexServer.handleCommand("callum: get revive");
        System.out.println(complexServer.handleCommand("callum: resurrect using the revive"));
        //check elf has been revived
        assertTrue(complexServer.handleCommand("callum: look").contains("elf"));
        //check revive is in storeroom - consumed from player inventory
        assertTrue(complexServer.gameLocations.get("storeroom").getArtefacts().containsKey("revive"));
        //check elf no longer exists in the storeroom
        assertFalse(complexServer.gameLocations.get("storeroom").getCharacters().containsKey("elf"));
    }

    @Test
    void testActionElementsInOtherInv(){
      complexServer.handleCommand("callum: goto forest");
      complexServer.handleCommand("beth: goto forest");
      complexServer.handleCommand("beth: get key");
      complexServer.handleCommand("callum: get hammer");
      complexServer.handleCommand("callum: goto cabin");
      complexServer.handleCommand("beth: goto cabin");
        complexServer.handleCommand("beth: get screwdriver");
      assertFalse(complexServer.handleCommand("beth: unlock da door w the HAMMER").contains("bash"));
      assertTrue(complexServer.handleCommand("callum: open trapdoor").contains("bash"));
    }

    @Test
    void testProducedFromAnywhere(){
        extServer.handleCommand("jaeren: goto forest");
        extServer.handleCommand("jaeren: goto riverbank");
        extServer.handleCommand("jaeren: get horn");
        extServer.handleCommand("jaeren: blow horn");
        extServer.handleCommand("jaeren: look");
        extServer.handleCommand("jaeren: blow horn");
        //producing an entity in the same location shouldn't change anything
        assertTrue(extServer.handleCommand("jaeren: look").contains("lumberjack"));
        extServer.handleCommand("jaeren: goto forest");
        extServer.handleCommand("jaeren: blow horn");
        assertTrue(extServer.handleCommand("jaeren: look").contains("lumberjack"));
        extServer.handleCommand("jaeren: goto riverbank");
        //entity should have been removed from the riverbank
        assertFalse(extServer.handleCommand("jaeren: look").contains("lumberjack"));

        extServer.handleCommand("jaeren: goto forest");
        extServer.handleCommand("alice: goto forest");
        String response = extServer.handleCommand("alice: look");

        //check game state is consistent across multiple players again
        assertTrue(response.contains("jaeren"));
        assertTrue(response.contains("lumberjack"));

        extServer.handleCommand("alice: goto cabin");
        response = extServer.handleCommand("jaeren: look");

        assertFalse(response.contains("alice"));
    }

    @Test
    void testLocationsArentReconsumable(){
      complexServer.handleCommand("tony: goto forest");
      complexServer.handleCommand("tony: get key");
      complexServer.handleCommand("tony: goto cabin");
      complexServer.handleCommand("tony: get screwdriver");
        complexServer.handleCommand("tony: unlock with key");
        assertTrue(complexServer.handleCommand("tony: look").contains("cellar"));
        complexServer.handleCommand("tony: lock with key");
        assertFalse(complexServer.handleCommand("tony: look").contains("cellar"));
        System.out.println(complexServer.handleCommand("tony: open the trapdoor"));
        //trying to lock the cellar twice should not be allowed
        assertFalse(complexServer.handleCommand("tony: lock with key").contains("You lock the trapdoor back up"));
    }

}
