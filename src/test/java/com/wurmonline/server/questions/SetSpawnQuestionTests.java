package com.wurmonline.server.questions;

import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static mod.wurmunlimited.Assert.bmlEqual;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SetSpawnQuestionTests extends BeastSummonerTest {
    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SummonerProfile profile = db.getProfileFor(summoner);
        assert profile != null;
        db.setSpawnFor(summoner, profile.spawnPointCentre, 5, -1);
    }

    @Test
    void testCancelChangesNothing() {
        SummonerProfile profile = db.getProfileFor(summoner);
        assert profile != null;
        VolaTile currentSpawn = profile.spawnPointCentre;
        int currentFloor = profile.floorLevel;
        int currentRange = profile.range;
        Properties properties = new Properties();
        properties.setProperty("cancel", "true");
        properties.setProperty("range", String.valueOf(profile.range + 1));
        VolaTile newTile = Zones.getOrCreateTile(123, 456, true);
        assert newTile != null;
        assert currentSpawn != newTile;
        new SetSpawnQuestion(gm, summoner, newTile, profile.floorLevel + 1).answer(properties);

        profile = db.getProfileFor(summoner);
        assert profile != null;
        assertEquals(currentSpawn, profile.spawnPointCentre);
        assertEquals(currentRange, profile.range);
        assertEquals(currentFloor, profile.floorLevel);
    }

    @Test
    void testTileUpdated() {
        SummonerProfile profile = db.getProfileFor(summoner);
        assert profile != null;
        VolaTile currentSpawn = profile.spawnPointCentre;
        int currentFloor = profile.floorLevel;
        int currentRange = profile.range;
        Properties properties = new Properties();
        properties.setProperty("submit", "true");
        properties.setProperty("range", String.valueOf(currentRange));
        VolaTile newTile = Zones.getOrCreateTile(123, 456, true);
        assert newTile != null;
        assert currentSpawn != newTile;
        new SetSpawnQuestion(gm, summoner, newTile, profile.floorLevel).answer(properties);

        profile = db.getProfileFor(summoner);
        assert profile != null;
        assertEquals(newTile, profile.spawnPointCentre);
        assertEquals(currentRange, profile.range);
        assertEquals(currentFloor, profile.floorLevel);
    }

    @Test
    void testRangeUpdated() {
        SummonerProfile profile = db.getProfileFor(summoner);
        assert profile != null;
        VolaTile currentSpawn = profile.spawnPointCentre;
        int currentFloor = profile.floorLevel;
        int newRange = profile.range + 1;
        Properties properties = new Properties();
        properties.setProperty("submit", "true");
        properties.setProperty("range", String.valueOf(newRange));
        new SetSpawnQuestion(gm, summoner, currentSpawn, profile.floorLevel).answer(properties);

        profile = db.getProfileFor(summoner);
        assert profile != null;
        assertEquals(currentSpawn, profile.spawnPointCentre);
        assertEquals(newRange, profile.range);
        assertEquals(currentFloor, profile.floorLevel);
    }

    @Test
    void testRangeMustBePositive() {
        SummonerProfile profile = db.getProfileFor(summoner);
        assert profile != null;
        VolaTile currentSpawn = profile.spawnPointCentre;
        int currentFloor = profile.floorLevel;
        int currentRange = profile.range;
        Properties properties = new Properties();
        properties.setProperty("submit", "true");
        properties.setProperty("range", "-1");
        new SetSpawnQuestion(gm, summoner, currentSpawn, profile.floorLevel).answer(properties);

        profile = db.getProfileFor(summoner);
        assert profile != null;
        assertEquals(currentSpawn, profile.spawnPointCentre);
        assertEquals(currentRange, profile.range);
        assertEquals(currentFloor, profile.floorLevel);
        assertThat(gm, receivedMessageContaining("must be 0 or greater"));
    }

    @Test
    void testRangeInvalid() {
        SummonerProfile profile = db.getProfileFor(summoner);
        assert profile != null;
        VolaTile currentSpawn = profile.spawnPointCentre;
        int currentFloor = profile.floorLevel;
        int currentRange = profile.range;
        Properties properties = new Properties();
        properties.setProperty("submit", "true");
        properties.setProperty("range", "abc");
        new SetSpawnQuestion(gm, summoner, currentSpawn, profile.floorLevel).answer(properties);

        profile = db.getProfileFor(summoner);
        assert profile != null;
        assertEquals(currentSpawn, profile.spawnPointCentre);
        assertEquals(currentRange, profile.range);
        assertEquals(currentFloor, profile.floorLevel);
        assertThat(gm, receivedMessageContaining("Invalid range"));
    }

    @Test
    void testFloorLevelUpdated() {
        SummonerProfile profile = db.getProfileFor(summoner);
        assert profile != null;
        VolaTile currentSpawn = profile.spawnPointCentre;
        int currentFloor = profile.floorLevel;
        int currentRange = profile.range;
        Properties properties = new Properties();
        properties.setProperty("submit", "true");
        properties.setProperty("range", String.valueOf(currentRange));
        new SetSpawnQuestion(gm, summoner, currentSpawn, profile.floorLevel + 1).answer(properties);

        profile = db.getProfileFor(summoner);
        assert profile != null;
        assertEquals(currentSpawn, profile.spawnPointCentre);
        assertEquals(currentRange, profile.range);
        assertEquals(currentFloor + 1, profile.floorLevel);
    }

    private void checkTile(int x, int y) {
        VolaTile tile = Zones.getTileOrNull(x, y, true);
        assert tile != null;
        assertEquals(1, tile.getItems().length);
    }

    @Test
    void testSurveyCreatesItems() {
        SummonerProfile profile = db.getProfileFor(summoner);
        assert profile != null;
        VolaTile currentSpawn = profile.spawnPointCentre;
        int currentFloor = profile.floorLevel;
        int currentRange = profile.range;
        Properties properties = new Properties();
        properties.setProperty("survey", "true");
        properties.setProperty("range", String.valueOf(currentRange));
        Question question = new SetSpawnQuestion(gm, summoner, currentSpawn, profile.floorLevel);
        question.sendQuestion();
        question.answer(properties);

        profile = db.getProfileFor(summoner);
        assert profile != null;
        assertEquals(currentSpawn, profile.spawnPointCentre);
        assertEquals(currentRange, profile.range);
        assertEquals(currentFloor, profile.floorLevel);
        assertThat(gm, bmlEqual());

        assertEquals(0, currentSpawn.getItems().length);
        int x = currentSpawn.getTileX() - currentRange;
        int y = currentSpawn.getTileY() - currentRange;
        while (x < currentSpawn.getTileX() + currentRange) {
            checkTile(x, y);
            x += 10;
        }
        while (y < currentSpawn.getTileY() + currentRange) {
            checkTile(x, y);
            y += 10;
        }
        while (x > currentSpawn.getTileX() - currentRange) {
            checkTile(x, y);
            x -= 10;
        }
        while (y > currentSpawn.getTileY() - currentRange) {
            checkTile(x, y);
            y -= 10;
        }
    }

    @Test
    void testSurveyCreatesItemsZeroRange() {
        SummonerProfile profile = db.getProfileFor(summoner);
        assert profile != null;
        VolaTile currentSpawn = profile.spawnPointCentre;
        int currentFloor = profile.floorLevel;
        int currentRange = profile.range;
        assert currentRange != 0;
        Properties properties = new Properties();
        properties.setProperty("survey", "true");
        properties.setProperty("range", String.valueOf(0));
        Question question = new SetSpawnQuestion(gm, summoner, currentSpawn, profile.floorLevel);
        question.sendQuestion();
        question.answer(properties);

        profile = db.getProfileFor(summoner);
        assert profile != null;
        assertEquals(currentSpawn, profile.spawnPointCentre);
        assertEquals(currentRange, profile.range);
        assertEquals(currentFloor, profile.floorLevel);

        assertEquals(1, currentSpawn.getItems().length);
    }

    @Test
    void testSurveyThenSubmitRemovesItems() {
        SummonerProfile profile = db.getProfileFor(summoner);
        assert profile != null;
        VolaTile currentSpawn = profile.spawnPointCentre;
        int currentFloor = profile.floorLevel;
        int currentRange = profile.range;
        assert currentRange != 0;
        Properties properties = new Properties();
        properties.setProperty("survey", "true");
        properties.setProperty("range", String.valueOf(0));
        Question question = new SetSpawnQuestion(gm, summoner, currentSpawn, profile.floorLevel);
        question.sendQuestion();
        question.answer(properties);

        profile = db.getProfileFor(summoner);
        assert profile != null;
        assertEquals(currentSpawn, profile.spawnPointCentre);
        assertEquals(currentRange, profile.range);
        assertEquals(currentFloor, profile.floorLevel);

        assertEquals(1, currentSpawn.getItems().length);

        properties.remove("survey");
        properties.setProperty("submit", "true");
        question.answer(properties);

        profile = db.getProfileFor(summoner);
        assert profile != null;
        assertEquals(currentSpawn, profile.spawnPointCentre);
        assertEquals(0, profile.range);
        assertEquals(currentFloor, profile.floorLevel);

        assertEquals(0, currentSpawn.getItems().length);
    }
}
