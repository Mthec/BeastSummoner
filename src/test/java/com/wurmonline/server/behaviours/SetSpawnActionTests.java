package com.wurmonline.server.behaviours;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.SetSpawnQuestion;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mod.wurmunlimited.Assert.bmlEqual;
import static mod.wurmunlimited.Assert.didNotReceiveBMLContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class SetSpawnActionTests extends BeastSummonerTest {
    private SetSpawnAction action;
    private final Action act = mock(Action.class);
    private Item wand;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wand = factory.createNewItem(ItemList.wandGM);
        SetSpawnAction.settingSpawn.clear();
        SetSpawnAction.settingSpawn.put(gm, summoner);
        action = new SetSpawnAction();
    }

    // getBehavioursFor

    @Test
    void testGetBehavioursFor() {
        List<ActionEntry> list = action.getBehavioursFor(gm, wand, 123, 123, true, 1);
        assertEquals(3, list.size());
        assertEquals("Spawn Point", list.get(0).getActionString());
        assertEquals("Set Spawn", list.get(1).getActionString());
        assertEquals("Cancel", list.get(2).getActionString());
    }

    @Test
    void testGetBehavioursForNotGM() {
        assertNull(action.getBehavioursFor(factory.createNewPlayer(), wand, 123, 123, true, 1));
    }

    @Test
    void testGetBehavioursForNotWand() {
        Item notWand = factory.createNewItem();
        assert !notWand.isWand();
        assertNull(action.getBehavioursFor(gm, notWand, 123, 123, true, 1));
    }

    @Test
    void testGetBehavioursForNotSettingSpawn() {
        SetSpawnAction.settingSpawn.clear();
        assertNull(action.getBehavioursFor(gm, wand, 123, 123, true, 1));
    }

    // action

    @Test
    void testSetSpawn() {
        assert SetSpawnAction.settingSpawn.containsKey(gm);
        VolaTile tile = Zones.getOrCreateTile(123, 123, true);
        assert tile != null;
        assertTrue(action.action(act, gm, wand, tile.getTileX(), tile.getTileY(), tile.isOnSurface(), -1, 1, action.getActionId(), 0f));
        assertEquals(0, SetSpawnAction.settingSpawn.size());
        new SetSpawnQuestion(gm, summoner, tile, -1).sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    void testSetSpawnNotGM() {
        Player player = factory.createNewPlayer();
        SetSpawnAction.settingSpawn.put(player, summoner);
        VolaTile tile = Zones.getOrCreateTile(123, 123, true);
        assert tile != null;
        assertTrue(action.action(act, player, wand, tile.getTileX(), tile.getTileY(), tile.isOnSurface(), -1, 1, action.getActionId(), 0f));
        assertEquals(2, SetSpawnAction.settingSpawn.size());
        assertThat(player, didNotReceiveBMLContaining("set the spawn"));
    }

    @Test
    void testSetSpawnNotWand() {
        assert SetSpawnAction.settingSpawn.containsKey(gm);
        VolaTile tile = Zones.getOrCreateTile(123, 123, true);
        assert tile != null;
        Item notWand = factory.createNewItem();
        assert !notWand.isWand();
        assertTrue(action.action(act, gm, notWand, tile.getTileX(), tile.getTileY(), tile.isOnSurface(), -1, 1, action.getActionId(), 0f));
        assertEquals(1, SetSpawnAction.settingSpawn.size());
        assertThat(gm, didNotReceiveBMLContaining("set the spawn"));
    }

    @Test
    void testSetSpawnNotSettingSpawn() {
        SetSpawnAction.settingSpawn.clear();
        VolaTile tile = Zones.getOrCreateTile(123, 123, true);
        assert tile != null;
        assertTrue(action.action(act, gm, wand, tile.getTileX(), tile.getTileY(), tile.isOnSurface(), -1, 1, action.getActionId(), 0f));
        assertEquals(0, SetSpawnAction.settingSpawn.size());
        assertThat(gm, didNotReceiveBMLContaining("set the spawn"));
    }
}
