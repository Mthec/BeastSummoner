package com.wurmonline.server.questions;

import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import mod.wurmunlimited.npcs.beastsummoner.db.BeastSummonerDatabase;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static mod.wurmunlimited.Assert.receivedBMLContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class BeastSummonerRenameTagsQuestionTests extends BeastSummonerTest {
    @Test
    void testTagsAppearInList() {
        new BeastSummonerRenameTagsQuestion(gm, Arrays.asList("tag1", "tag2", "tag3")).sendQuestion();

        assertThat(gm, receivedBMLContaining("tag1"));
        assertThat(gm, receivedBMLContaining("tag2"));
        assertThat(gm, receivedBMLContaining("tag3"));
    }

    // answer

    @Test
    void testRenameSingleTag() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), "tag1");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag2");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag3");

        List<String> tags = Arrays.asList("tag1", "tag2", "tag3");
        Properties properties = new Properties();
        properties.setProperty("t0", "tag4");
        properties.setProperty("rename", "true");
        new BeastSummonerRenameTagsQuestion(gm, tags).answer(properties);

        List<String> allTags = db.getAllTags();
        assertEquals(3, allTags.size());
        assertFalse(allTags.contains("tag1"));
        assertTrue(allTags.contains("tag4"));
        assertThat(gm, receivedMessageContaining("1 tag was renamed"));
    }

    @Test
    void testRenameMultipleTags() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), "tag1");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag2");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag3");

        List<String> tags = Arrays.asList("tag1", "tag2", "tag3");
        Properties properties = new Properties();
        properties.setProperty("t0", "tag4");
        properties.setProperty("t2", "tag5");
        properties.setProperty("rename", "true");
        new BeastSummonerRenameTagsQuestion(gm, tags).answer(properties);

        List<String> allTags = db.getAllTags();
        assertEquals(3, allTags.size());
        assertFalse(allTags.contains("tag1"));
        assertFalse(allTags.contains("tag3"));
        assertTrue(allTags.contains("tag4"));
        assertTrue(allTags.contains("tag5"));
        assertThat(gm, receivedMessageContaining("2 tags were renamed"));
    }

    @Test
    void testNotRenamedIfNoChangesMade() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), "tag1");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag2");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag3");

        List<String> tags = Arrays.asList("tag1", "tag2", "tag3");
        Properties properties = new Properties();
        properties.setProperty("rename", "true");
        new BeastSummonerRenameTagsQuestion(gm, tags).answer(properties);

        List<String> allTags = db.getAllTags();
        assertEquals(3, allTags.size());
        assertTrue(allTags.contains("tag1"));
        assertTrue(allTags.contains("tag2"));
        assertTrue(allTags.contains("tag3"));
        assertThat(gm, receivedMessageContaining("0 tags were renamed"));
    }
}
