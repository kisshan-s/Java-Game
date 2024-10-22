package edu.uob.Actions;

import edu.uob.Entities.GameEntity;

import java.util.ArrayList;
import java.util.Set;

public class GameAction
{
    /**
     * key phrases to check for in user input
     */
    private final Set<String> keyPhrases;
    private final Set<String> subjects;
    private final Set<String> consumedEntities;
    private final Set<String> producedEntities;
    private final String narration;

    /**
     * constructor method to store gameaction items
     */
    public GameAction(Set<String> keyPhrases, Set<String> subjects, Set<String> consumedEntities, Set<String> producedEntities, String narration) {
        this.keyPhrases = keyPhrases;
        this.subjects = subjects;
        this.consumedEntities = consumedEntities;
        this.producedEntities = producedEntities;
        this.narration = narration;
    }

    /**
     * getter methods to obtain the info stored in this GameAction
     * @return GameAction items
     */

    public Set<String> getKeyPhrases() {
        return keyPhrases;
    }

    public Set<String> getSubjects() {
        return subjects;
    }

    public Set<String> getConsumedEntities() {
        return consumedEntities;
    }

    public Set<String> getProducedEntities() {
        return producedEntities;
    }

    public String getNarration() {
        return narration;
    }
}
