package com.itcag.rockwell.tagger;

import com.itcag.rockwell.lang.Semtoken;
import com.itcag.rockwell.lang.Tag;
import com.itcag.rockwell.lang.Token;
import com.itcag.rockwell.tagger.lang.Conditions;
import com.itcag.rockwell.tagger.lang.Match;
import com.itcag.rockwell.tagger.lang.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

/**
 * <p>This class validates a single {@link com.itcag.rockwell.lang.Token token}.</p>
 */
public class TokenAnalyzer {

    private final String sentenceID = UUID.randomUUID().toString().replace("-", "").trim();
        
    private final Processor processor;
    
    private ArrayList<State> currentStates = new ArrayList<>();
    private ArrayList<State> newStates = new ArrayList<>();
    
    private final HashMap<String, State> rejecting = new HashMap<>();
    private final ArrayList<State> matches = new ArrayList<>();
        
    /**
     * @param processor Instance of the {@link com.itcag.rockwell.tagger.Processor Processor} class.
     */
    public TokenAnalyzer(Processor processor) {
        this.processor = processor;
    }

    /**
     * @param token Instance of the {@link com.itcag.rockwell.lang.Token Token} class.
     * @throws Exception if anything goes wrong.
     */
    public void analyze(Token token) throws Exception {
        
        /**
         * Some tokens have part of speech set during the tokenization.
         */
        if (token.getAlternatives().isEmpty()) {
            addNewStates(processor.getStates(currentStates, token, true));
        } else {

            if (token instanceof Semtoken || token instanceof Match) {
                addNewStates(processor.getStates(currentStates, token, true));
            }

            /**
             * Each alternative is processed separately.
             */
            for (Token alternative : token.getAlternatives()) {
                Token newToken = new Token(alternative.getWord(), alternative.getPos(), alternative.getLemma(), token.getIndex());
                /**
                 * Only the first alternative is used for checking the quodlibet.
                 */
                if (alternative.equals(token.getAlternatives().get(0))) {
                    addNewStates(processor.getStates(currentStates, newToken, true));
                } else {
                    addNewStates(processor.getStates(currentStates, newToken, false));
                }
            }

        }

        validateStates(newStates);

        /**
         * The current states are updated for further processing.
         */
        currentStates = newStates;
        newStates = new ArrayList<>();

    }
    
    private void addNewStates(ArrayList<State> newStates) {
        for (State newState : newStates) {
            this.newStates.add(newState);
        }
    }
    
    private void validateStates(ArrayList<State> newStates) {
        
        HashMap<String, ArrayList<State>> validator = new HashMap<>();
        Iterator<State> stateIterator = newStates.iterator();
        while (stateIterator.hasNext()) {
            State newState = stateIterator.next();
            /**
             * Some rules create multiple identical states:
             * if POS is not checked, and a token has multiple POS,
             * a state will be created for each of them.
             * For example, of "price" is matched by lemma,
             * but it can be NN1, VVB or VVI - three states are created.
             * However, we don't care for the POS,
             * so one rule is sufficient to continue.
             */
            if (isValid(newState, validator)) {
                if (validator.containsKey(newState.getConditionId())) {
                    validator.get(newState.getConditionId()).add(newState);
                } else {
                    validator.put(newState.getConditionId(), new ArrayList<>(Arrays.asList(newState)));
                }
                /**
                 * For each token the states are checked
                 * to see if any tags were identified.
                 */
                if (newState.getState() == Conditions.FINAL_STATE_CODE) {
                    if (newState.getIdToBeRejected() != null) {
                        this.rejecting.put(newState.getIdToBeRejected(), newState.getCopy());
                    } else {
                        this.matches.add(newState.getCopy());
                    }
                    stateIterator.remove();
                }
            } else {
                stateIterator.remove();
            }
        }
            
    }
    
    private boolean isValid(State newState, HashMap<String, ArrayList<State>> validator) {
        
        if (!validator.containsKey(newState.getConditionId())) return true;
        
        for (State state : validator.get(newState.getConditionId())) {
            if (newState.getState() == state.getState()) {
                return false;
            }
        }
        
        return true;
        
    }

    public ArrayList<Tag> getTags(boolean allowMultipleTags) {
        
        ArrayList<Tag> retVal = new ArrayList<>();

        if (this.matches.size() == 1) {
            State match = this.matches.get(0);
            Tag tag = new Tag(match.getTag(), match.getScript(), match.getFirstMatch(), match.getLastMatch());
            tag.setSentenceId(sentenceID);
            retVal.add(tag);
            return retVal;
        }

        Iterator<State> matchIterator = this.matches.iterator();
        while (matchIterator.hasNext()) {
            State match = matchIterator.next();
            if (!this.rejecting.containsKey(match.getConditionId())) {
                if (!allowMultipleTags && isIncluded(match, retVal)) continue;
                Tag tag = new Tag(match.getTag(), match.getScript(), match.getFirstMatch(), match.getLastMatch());
                tag.setSentenceId(sentenceID);
                retVal.add(tag);
            }
        }

        return retVal;
        
    }
    
    private boolean isIncluded(State match, ArrayList<Tag> tags) {
        
        if (tags.isEmpty()) return false;
        
        Iterator<Tag> tagIterator = tags.iterator();
        while (tagIterator.hasNext()) {
            Tag tag = tagIterator.next();
            if (match.getStart() == tag.getStart() && match.getEnd() == tag.getEnd()) {
                return true;
            } else if (match.getStart() > tag.getStart() && match.getEnd() < tag.getEnd()) {
                return true;
            } else if (match.getStart() == tag.getStart() && match.getEnd() < tag.getEnd()) {
                return true;
            } else if (match.getStart() > tag.getStart() && match.getEnd() == tag.getEnd()) {
                return true;
            } else if (match.getStart() <= tag.getStart() && match.getEnd() >= tag.getEnd()) {
                tagIterator.remove();
            }
        }

        return false;
        
    }
    
}
