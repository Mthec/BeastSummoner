package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.BeastSummonerManagementQuestion;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTemplate;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.Collections;
import java.util.List;

public class ManageBeastSummonerAction implements ModAction, BehaviourProvider, ActionPerformer {
    private final short actionId;
    private final ActionEntry actionEntry;

    public ManageBeastSummonerAction() {
        actionId = (short)ModActions.getNextActionId();
        actionEntry = new ActionEntryBuilder(actionId, "Manage", "managing beast summoner").build();
        ModActions.registerAction(actionEntry);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        if (subject.isWand() && performer.getPower() >= 2 && BeastSummonerTemplate.is(target))
            return Collections.singletonList(actionEntry);
        return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        if (num == actionId && source.isWand() && performer.getPower() >= 2 && BeastSummonerTemplate.is(target)) {
            new BeastSummonerManagementQuestion(performer, target).sendQuestion();
        }

        return true;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
