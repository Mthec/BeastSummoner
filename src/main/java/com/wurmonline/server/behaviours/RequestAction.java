package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.BeastSummonerRequestQuestion;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTemplate;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class RequestAction implements ModAction, ActionPerformer, BehaviourProvider {
    private final short actionId;
    private final List<ActionEntry> actionEntry;

    public RequestAction() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Request", "requesting").build();
        ModActions.registerAction(actionEntry);
        this.actionEntry = Collections.singletonList(actionEntry);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Creature target) {
        if (BeastSummonerTemplate.is(target)) {
            return actionEntry;
        }
        return null;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        return getBehavioursFor(performer, target);
    }

    @Override
    public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
        if (performer.isPlayer() && BeastSummonerTemplate.is(target)) {
            target.turnTowardsCreature(performer);

            try {
                target.getStatus().savePosition(target.getWurmId(), false, target.getStatus().getZoneId(), true);
            } catch (IOException ignored) {}

            new BeastSummonerRequestQuestion((Player)performer, target).sendQuestion();
        }

        return true;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
