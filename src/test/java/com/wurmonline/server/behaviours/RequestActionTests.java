package com.wurmonline.server.behaviours;

import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.BeastSummonerRequestQuestion;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mod.wurmunlimited.Assert.bmlEqual;
import static mod.wurmunlimited.Assert.didNotReceiveBMLContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class RequestActionTests extends BeastSummonerTest {
    private Player player;
    private RequestAction action;
    private final Action act = mock(Action.class);

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        player = factory.createNewPlayer();
        action = new RequestAction();
    }

    // getBehavioursFor

    @Test
    void testGetBehavioursFor() {
        List<ActionEntry> list = action.getBehavioursFor(player, summoner);
        assertEquals(1, list.size());
        assertEquals("Request", list.get(0).getActionString());
    }

    @Test
    void testGetBehavioursForWithItem() {
        List<ActionEntry> list = action.getBehavioursFor(player, factory.createNewItem(), summoner);
        assertEquals(1, list.size());
        assertEquals("Request", list.get(0).getActionString());
    }

    @Test
    void testGetBehavioursForNotBeastSummoner() {
        assertNull(action.getBehavioursFor(player, factory.createNewCreature()));
    }

    // action

    @Test
    void testRequest() {
        assertTrue(action.action(act, player, summoner, (short)1, 0f));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlEqual());
    }

    @Test
    void testRequestWithItem() {
        assertTrue(action.action(act, player, factory.createNewItem(), summoner, (short)1, 0f));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlEqual());
    }

    @Test
    void testRequestNotBeastSummoner() {
        assertTrue(action.action(act, player, factory.createNewCreature(), (short)1, 0f));
        assertThat(player, didNotReceiveBMLContaining("to summon"));
    }
}
