package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.Floor;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class CancelSetSpawnAction implements ModAction, ActionPerformer {
    private final short actionId;

    public CancelSetSpawnAction() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Cancel", "cancelling").build();
        ModActions.registerAction(actionEntry);
        SetSpawnAction.cancelAction = actionEntry;
    }

    private boolean doAction(Creature performer) {
        Creature summoner = SetSpawnAction.settingSpawn.remove(performer);
        if (summoner != null) {
            performer.getCommunicator().sendNormalServerMessage("You will no longer set the spawn for " + summoner.getName() + ".");
        }

        return true;
    }

    public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int floorLevel, int tile, short num, float counter) {
        return doAction(performer);
    }

    public boolean action(Action action, Creature performer, Item source, boolean onSurface, Floor floor, int encodedTile, short num, float counter) {
        return doAction(performer);
    }

    public boolean action(Action action, Creature performer, Item source, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
        return doAction(performer);
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
