package com.wurmonline.server.questions;

import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import mod.wurmunlimited.npcs.beastsummoner.db.BeastSummonerDatabase;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class BeastSummonerEditTagsQuestionTests extends BeastSummonerTest {
    @Test
    void testTagsAppearInList() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), "tag1");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag2");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag3");

        new BeastSummonerEditTagsQuestion(gm).sendQuestion();

        assertThat(gm, receivedBMLContaining("tag1"));
        assertThat(gm, receivedBMLContaining("tag2"));
        assertThat(gm, receivedBMLContaining("tag3"));
    }

    // answer

    @Test
    void testNoTags() {
        assertDoesNotThrow(() -> new BeastSummonerEditTagsQuestion(gm).answer(new Properties()));
    }

    @Test
    void testRemoveSingleTag() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), "tag1");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag2");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag3");

        Properties properties = new Properties();
        properties.setProperty("s0", "true");
        properties.setProperty("remove", "true");
        new BeastSummonerEditTagsQuestion(gm).answer(properties);

        List<String> allTags = db.getAllTags();
        assertEquals(2, allTags.size());
        assertFalse(allTags.contains("tag1"));
        assertThat(gm, receivedMessageContaining("1 tag was removed"));
    }

    @Test
    void testRemoveMultipleTags() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), "tag1");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag2");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag3");

        Properties properties = new Properties();
        properties.setProperty("s0", "true");
        properties.setProperty("s2", "true");
        properties.setProperty("remove", "true");
        new BeastSummonerEditTagsQuestion(gm).answer(properties);

        List<String> allTags = db.getAllTags();
        assertEquals(1, allTags.size());
        assertTrue(allTags.contains("tag2"));
        assertThat(gm, receivedMessageContaining("2 tags were removed"));
    }

    @Test
    void testNotRemovedIfNoneSelected() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), "tag1");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag2");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag3");

        Properties properties = new Properties();
        properties.setProperty("remove", "true");
        new BeastSummonerEditTagsQuestion(gm).answer(properties);

        List<String> allTags = db.getAllTags();
        assertEquals(3, allTags.size());
        assertTrue(allTags.contains("tag1"));
        assertTrue(allTags.contains("tag2"));
        assertTrue(allTags.contains("tag3"));
        assertThat(gm, receivedMessageContaining("0 tags were removed"));
    }

    @Test
    void testRenameSingleSelected() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), "tag1");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag2");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag3");

        Properties properties = new Properties();
        properties.setProperty("s0", "true");
        properties.setProperty("rename", "true");
        new BeastSummonerEditTagsQuestion(gm).answer(properties);
        new BeastSummonerRenameTagsQuestion(gm, Collections.singletonList("tag1")).sendQuestion();

        List<String> allTags = db.getAllTags();
        assertEquals(3, allTags.size());
        assertTrue(allTags.contains("tag1"));
        assertTrue(allTags.contains("tag2"));
        assertTrue(allTags.contains("tag3"));
        assertThat(gm, bmlEqual());
        assertEquals(0, factory.getCommunicator(gm).getMessages().length);
    }

    @Test
    void testRenameMultipleSelected() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), "tag1");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag2");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag3");

        Properties properties = new Properties();
        properties.setProperty("s0", "true");
        properties.setProperty("s2", "true");
        properties.setProperty("rename", "true");
        new BeastSummonerEditTagsQuestion(gm).answer(properties);
        new BeastSummonerRenameTagsQuestion(gm, Arrays.asList("tag1", "tag3")).sendQuestion();

        List<String> allTags = db.getAllTags();
        assertEquals(3, allTags.size());
        assertTrue(allTags.contains("tag1"));
        assertTrue(allTags.contains("tag2"));
        assertTrue(allTags.contains("tag3"));
        assertThat(gm, bmlEqual());
        assertEquals(0, factory.getCommunicator(gm).getMessages().length);
    }
}
