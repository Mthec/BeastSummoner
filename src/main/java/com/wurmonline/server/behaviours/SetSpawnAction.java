package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.SetSpawnQuestion;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.Floor;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SetSpawnAction implements ModAction, ActionPerformer, BehaviourProvider {
    private static final Logger logger = Logger.getLogger(SetSpawnAction.class.getName());
    static final Map<Creature, Creature> settingSpawn = new HashMap<>();
    static ActionEntry cancelAction = null;
    private final short actionId;
    private final ActionEntry actionEntry;
    private final List<ActionEntry> actionEntries = new ArrayList<>();

    public SetSpawnAction() {
        actionId = (short)ModActions.getNextActionId();
        actionEntry = new ActionEntryBuilder(actionId, "Cancel", "cancelling").build();
        ModActions.registerAction(actionEntry);
    }


    private List<ActionEntry> getBehaviours(Creature performer, Item item) {
        if (item.isWand() && performer.getPower() >= 2 && settingSpawn.get(performer) != null) {
            if (actionEntries.isEmpty()) {
                actionEntries.add(new ActionEntry((short)-2, "Spawn Point", "setting spawn point"));
                actionEntries.add(actionEntry);
                if (cancelAction == null) {
                    logger.warning("Cancel Action was null, please report.");
                } else {
                    actionEntries.add(cancelAction);
                }
            }
            return actionEntries;
        }

        return null;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, int tilex, int tiley, boolean onSurface, int tile) {
        return getBehaviours(performer, item);
    }

    // Seems to be for inside mines.  What is the point of onSurface in other methods then?
    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, int tilex, int tiley, boolean onSurface, int tile, int dir) {
        return getBehaviours(performer, item);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, boolean onSurface, Floor floor) {
        return getBehaviours(performer, item);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, boolean onSurface, BridgePart bridgePart) {
        return getBehaviours(performer, item);
    }

    private boolean doAction(Action action, short num, Creature performer, Item source, VolaTile tile, int floorLevel) {
        if (source.isWand() && performer.getPower() >= 2) {
            Creature settingFor = settingSpawn.remove(performer);
            if (settingFor != null) {
                new SetSpawnQuestion(performer, settingFor, tile, floorLevel).sendQuestion();
            }
        }

        return true;
    }

    @SuppressWarnings("DuplicatedCode")
    public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int floorLevel, int tile, short num, float counter) {
        VolaTile volaTile = Zones.getOrCreateTile(tilex, tiley, onSurface);
        if (volaTile == null) {
            performer.getCommunicator().sendAlertServerMessage("You could not be located.");
            logger.warning("Could not find or create tile (" + tile + ") at " + tilex + " - " + tiley + " surfaced=" + onSurface);
            return true;
        }

        if (!onSurface)
            floorLevel = 0;

        return doAction(action, num, performer, source, volaTile, floorLevel);
    }

    public boolean action(Action action, Creature performer, Item source, boolean onSurface, Floor floor, int encodedTile, short num, float counter) {
        return doAction(action, num, performer, source, floor.getTile(), floor.getFloorLevel());
    }

    public boolean action(Action action, Creature performer, Item source, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
        return doAction(action, num, performer, source, bridgePart.getTile(), bridgePart.getFloorLevel());
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
