package com.wurmonline.server.behaviours;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.Floor;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class CancelSetSpawnActionTests extends BeastSummonerTest {
    private CancelSetSpawnAction action;
    private final Action act = mock(Action.class);
    private Item wand;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wand = factory.createNewItem(ItemList.wandGM);
        SetSpawnAction.settingSpawn.clear();
        action = new CancelSetSpawnAction();
        SetSpawnAction.settingSpawn.put(gm, summoner);
    }

    @Test
    void testCancel() {
        assert SetSpawnAction.settingSpawn.containsKey(gm);
        assertTrue(action.action(act, gm, wand, 123, 123, true, -1, 1, action.getActionId(), 0f));
        assertTrue(SetSpawnAction.settingSpawn.isEmpty());
    }

    @Test
    void testCancelFloor() {
        assert SetSpawnAction.settingSpawn.containsKey(gm);
        assertTrue(action.action(act, gm, wand, true, mock(Floor.class), 1, action.getActionId(), 0f));
        assertTrue(SetSpawnAction.settingSpawn.isEmpty());
    }

    @Test
    void testCancelBridge() {
        assert SetSpawnAction.settingSpawn.containsKey(gm);
        assertTrue(action.action(act, gm, wand, true, mock(BridgePart.class), 1, action.getActionId(), 0f));
        assertTrue(SetSpawnAction.settingSpawn.isEmpty());
    }
}
