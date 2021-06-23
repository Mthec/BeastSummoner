package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTemplate;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class StartSetSpawnAction implements ModAction, ActionPerformer, BehaviourProvider {
    private static final Logger logger = Logger.getLogger(SetSpawnAction.class.getName());
    private final short actionId;
    private final List<ActionEntry> actionEntry;
    private final List<ActionEntry> actionEntries = new ArrayList<>();

    public StartSetSpawnAction() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Set Spawn", "setting spawn").build();
        ModActions.registerAction(actionEntry);
        this.actionEntry = Collections.singletonList(actionEntry);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        if (subject.isWand() && performer.getPower() >= 2 && BeastSummonerTemplate.is(target)) {
            return actionEntry;
        }

        return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        if (source.isWand() && performer.getPower() >= 2 && BeastSummonerTemplate.is(target)) {
            SetSpawnAction.settingSpawn.put(performer, target);
            performer.getCommunicator().sendNormalServerMessage("You can now set the spawn for " + target.getName() + " by right-clicking a tile.");
        }

        return true;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
