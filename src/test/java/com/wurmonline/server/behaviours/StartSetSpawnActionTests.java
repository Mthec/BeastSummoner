package com.wurmonline.server.behaviours;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class StartSetSpawnActionTests extends BeastSummonerTest {
    private StartSetSpawnAction action;
    private final Action act = mock(Action.class);
    private Item wand;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wand = factory.createNewItem(ItemList.wandGM);
        SetSpawnAction.settingSpawn.clear();
        action = new StartSetSpawnAction();
    }

    // getBehavioursFor

    @Test
    void testGetBehavioursFor() {
        List<ActionEntry> list = action.getBehavioursFor(gm, wand, summoner);
        assertEquals(1, list.size());
        assertEquals("Set Spawn", list.get(0).getActionString());
    }

    @Test
    void testGetBehavioursForNotGM() {
        assertNull(action.getBehavioursFor(factory.createNewPlayer(), wand, summoner));
    }

    @Test
    void testGetBehavioursForNotWand() {
        Item notWand = factory.createNewItem();
        assert !notWand.isWand();
        assertNull(action.getBehavioursFor(gm, notWand, summoner));
    }

    @Test
    void testGetBehavioursForNotBeastSummoner() {
        assertNull(action.getBehavioursFor(gm, wand, factory.createNewCreature()));
    }

    // action

    @Test
    void testStartSetSpawn() {
        assert SetSpawnAction.settingSpawn.isEmpty();
        assertTrue(action.action(act, gm, wand, summoner, action.getActionId(), 0f));
        assertEquals(1, SetSpawnAction.settingSpawn.size());
        assertTrue(SetSpawnAction.settingSpawn.containsKey(gm));
        assertTrue(SetSpawnAction.settingSpawn.containsValue(summoner));
        assertThat(gm, receivedMessageContaining("now set the spawn"));
    }

    @Test
    void testStartSetSpawnNotGM() {
        Player player = factory.createNewPlayer();
        assert SetSpawnAction.settingSpawn.isEmpty();
        assertTrue(action.action(act, player, wand, summoner, action.getActionId(), 0f));
        assertEquals(0, SetSpawnAction.settingSpawn.size());
        assertThat(player, didNotReceiveMessageContaining("now set the spawn"));
    }

    @Test
    void testStartSetSpawnNotWand() {
        Item notWand = factory.createNewItem();
        assert !notWand.isWand();
        assert SetSpawnAction.settingSpawn.isEmpty();
        assertTrue(action.action(act, gm, notWand, summoner, action.getActionId(), 0f));
        assertEquals(0, SetSpawnAction.settingSpawn.size());
        assertThat(gm, didNotReceiveMessageContaining("now set the spawn"));
    }

    @Test
    void testStartSetSpawnNotBeastSummoner() {
        assert SetSpawnAction.settingSpawn.isEmpty();
        assertTrue(action.action(act, gm, wand, factory.createNewPlayer(), action.getActionId(), 0f));
        assertEquals(0, SetSpawnAction.settingSpawn.size());
        assertThat(gm, didNotReceiveMessageContaining("now set the spawn"));
    }
}
