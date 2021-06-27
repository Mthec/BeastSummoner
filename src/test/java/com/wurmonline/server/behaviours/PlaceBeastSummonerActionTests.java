package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.PlaceBeastSummonerQuestion;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.Assert;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class PlaceBeastSummonerActionTests extends BeastSummonerTest {
    private final Action act = mock(Action.class);
    private Item wand;
    private short actionId;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wand = factory.createNewItem(ItemList.wandGM);
        actionId = menu.getBehavioursFor(gm, wand, 1, 1, true, 1).get(1).getNumber();
    }

    // getBehavioursFor

    @Test
    void testCorrectBehaviourReceived() {
        List<ActionEntry> entries = menu.getBehavioursFor(gm, wand, 0, 0, true, 0);
        assertEquals(2, entries.size());
        assertEquals("Place Npc", entries.get(0).getActionString());
        assertEquals("Beast Summoner", entries.get(1).getActionString());
    }

    @Test
    void testPlayersDoNotGetOption() {
        Player player = factory.createNewPlayer();
        assert player.getPower() < 2;
        List<ActionEntry> entries = menu.getBehavioursFor(player, wand, 0, 0, true, 0);
        assertNull(entries);
    }

    @Test
    void testWandRequired() {
        Item item = factory.createNewItem();
        assert !item.isWand();
        List<ActionEntry> entries = menu.getBehavioursFor(gm, item, 0, 0, true, 0);
        assertNull(entries);
    }

    // Action

    @Test
    void testQuestionReceived() throws NoSuchFieldException, IllegalAccessException {
        assertTrue(menu.action(act, gm, wand, 0, 0, true,  0, 0, actionId, 0f));
        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        new PlaceBeastSummonerQuestion(gm, Objects.requireNonNull(Zones.getTileOrNull(0, 0, true)), 0).sendQuestion();

        // To account for random gender.
        String[] bml = factory.getCommunicator(gm).getBml();
        List<String> fixed = new ArrayList<>();
        for (String b : bml) {
            fixed.add(b.replace(";selected=\"true\"", ""));
        }
        ReflectionUtil.setPrivateField(factory.getCommunicator(gm), FakeCommunicator.class.getDeclaredField("bml"), fixed);

        assertThat(gm, Assert.bmlEqual());
    }

    @Test
    void testPlayersDoNotReceiveBML() {
        Player player = factory.createNewPlayer();
        assert player.getPower() < 2;
        assertTrue(menu.action(act, player, wand, 0, 0, true, 0, 0, actionId, 0f));
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testWandRequiredForBML() {
        Item item = factory.createNewItem();
        assert !item.isWand();
        assertTrue(menu.action(act, gm, item, 0, 0, true, 0, 0, (short)1, 0f));
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testIncorrectTileInformation() {
        assertTrue(menu.action(act, gm, wand, -250, -250, true, 0, 0, (short)1, 0f));
        assertEquals(1, factory.getCommunicator(gm).getMessages().length);
        assertThat(gm, receivedMessageContaining("not be located"));
    }
}
