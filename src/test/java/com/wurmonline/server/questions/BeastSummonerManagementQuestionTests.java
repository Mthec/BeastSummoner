package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.CreatureTemplateFactory;
import com.wurmonline.server.creatures.CreatureTemplateIds;
import com.wurmonline.server.creatures.FakeCreatureStatus;
import com.wurmonline.server.creatures.NoSuchCreatureTemplateException;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.items.Trade;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import mod.wurmunlimited.npcs.beastsummoner.db.BeastSummonerDatabase;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static mod.wurmunlimited.npcs.ModelSetter.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class BeastSummonerManagementQuestionTests extends BeastSummonerTest {
    private static final String tag = "MyTag";
    private static final String differentTag = "different";

    @Test
    void testProperlyGetsCurrentTag() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(summoner, tag);

        new BeastSummonerManagementQuestion(gm, summoner).sendQuestion();
        assertThat(gm, receivedBMLContaining(tag));
    }

    @Test
    void testContainsAllTags() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), "tag1");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag2");
        db.updateTagFor(factory.createNewBeastSummoner(), "tag3");

        new BeastSummonerManagementQuestion(gm, summoner).sendQuestion();
        assertThat(gm, receivedBMLContaining("tag1,tag2,tag3"));
    }

    @Test
    void testProperlyGetFace() throws SQLException {
        long face = 24680;
        BeastSummonerMod.mod.faceSetter.setFaceFor(summoner, face);
        BeastSummonerMod.mod.modelSetter.setModelFor(summoner, HUMAN_MODEL_NAME);

        new BeastSummonerManagementQuestion(gm, summoner).sendQuestion();
        assertThat(gm, receivedBMLContaining(face + "\";id=\"face\""));
    }

    @Test
    void testProperlyGetFaceIfNotHuman() throws SQLException {
        assert BeastSummonerMod.mod.faceSetter.getFaceFor(summoner) == null;
        BeastSummonerMod.mod.modelSetter.setModelFor(summoner, TRADER_MODEL_NAME);

        new BeastSummonerManagementQuestion(gm, summoner).sendQuestion();
        assertThat(gm, receivedBMLContaining("\"\";id=\"face\""));
    }

    @Test
    void testProperlyGetsModelTrader() throws SQLException {
        BeastSummonerMod.mod.modelSetter.setModelFor(summoner, TRADER_MODEL_NAME);

        new BeastSummonerManagementQuestion(gm, summoner).sendQuestion();
        assertThat(gm, receivedBMLContaining("id=\"trader\";text=\"Trader\";selected=\"true\""));
        assertThat(gm, receivedBMLContaining("text=\"\";id=\"custom_model\""));
    }

    @Test
    void testProperlyGetsModelHuman() throws SQLException {
        BeastSummonerMod.mod.modelSetter.setModelFor(summoner, HUMAN_MODEL_NAME);

        new BeastSummonerManagementQuestion(gm, summoner).sendQuestion();
        assertThat(gm, receivedBMLContaining("id=\"human\";text=\"Human\";selected=\"true\""));
        assertThat(gm, receivedBMLContaining("text=\"\";id=\"custom_model\""));
    }

    @Test
    void testProperlyGetsModel() throws SQLException {
        String model = "custom.model";
        BeastSummonerMod.mod.modelSetter.setModelFor(summoner, model);

        new BeastSummonerManagementQuestion(gm, summoner).sendQuestion();
        assertThat(gm, receivedBMLContaining("id=\"custom\";text=\"Custom\";selected=\"true\""));
        assertThat(gm, receivedBMLContaining(model + "\";id=\"custom_model\""));
    }

    // answer

    @Test
    void testSetName() {
        assert BeastSummonerMod.namePrefix.equals("Beast_Summoner");
        String name = StringUtilities.raiseFirstLetter("MyName");
        String newName = "Beast_Summoner_" + name;
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", name);
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(newName, summoner.getName());
        assertEquals(newName, ((FakeCreatureStatus)summoner.getStatus()).savedName);
        assertThat(gm, receivedMessageContaining("will now be known as " + newName));
    }

    @Test
    void testSetNameDifferentPrefix() {
        BeastSummonerMod.namePrefix = "MyPrefix";
        assert !summoner.getName().startsWith(BeastSummonerMod.namePrefix);
        String name = StringUtilities.raiseFirstLetter("MyName");
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", name);
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals("MyPrefix_" + name, summoner.getName());
        assertThat(gm, receivedMessageContaining("will now be known as MyPrefix_" + name));
    }

    @Test
    void testSetNameIllegalCharacters() {
        assert BeastSummonerMod.namePrefix.equals("Beast_Summoner");
        String name = summoner.getName();
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", "%Name");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertNotEquals(name, summoner.getName());
        assertThat(gm, receivedMessageContaining(name + " didn't like that name"));
        assertThat(gm, receivedMessageContaining(name + " will now be known as "));
    }

    @Test
    void testSetNameRandomWhenBlank() {
        assert BeastSummonerMod.namePrefix.equals("Beast_Summoner");
        String name = summoner.getName();
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", "");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertNotEquals(name, summoner.getName());
        assertTrue(summoner.getName().startsWith("Beast_Summoner_"));
        assertThat(gm, receivedMessageContaining(name + " chose a new name"));
        assertThat(gm, receivedMessageContaining(name + " will now be known as "));
    }

    @Test
    void testSetNameNoMessageOnSameName() {
        assert BeastSummonerMod.namePrefix.equals("Beast_Summoner");
        String name = "Name";
        summoner.setName("Beast_Summoner_" + name);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", name);
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals("Beast_Summoner_" + name, summoner.getName());
        assertThat(gm, didNotReceiveMessageContaining("will now be known as " + name));
    }

    @Test
    void testNothingChangesIfNoSettingsAreAltered() throws NoSuchCreatureTemplateException, SQLException {
        assert db.getTagFor(summoner).equals("");
        db.addOption(summoner, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.PHEASANT_CID), 1, 1, Collections.emptySet());
        String name = summoner.getName();

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", name.substring(15));
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals("", db.getTagFor(summoner));
        assertEquals(1, Objects.requireNonNull(db.getOptionsFor(summoner)).size());
        assertNull(Objects.requireNonNull(db.getProfileFor(summoner)).currency);
        assertEquals(name, summoner.getName());
        assertThat(gm, didNotReceiveMessageContaining("takes a new form"));
        assertThat(gm, didNotReceiveMessageContaining("known as"));
    }

    @Test
    void testCustomizeFaceSent() throws SQLException {
        BeastSummonerMod.mod.modelSetter.setModelFor(summoner, HUMAN_MODEL_NAME);
        long oldFace = 112358;
        BeastSummonerMod.mod.faceSetter.setFaceFor(summoner, oldFace);
        Properties properties = new Properties();
        properties.setProperty("face", "");
        properties.setProperty("model", "human");
        properties.setProperty("confirm", "true");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(oldFace, (long)BeastSummonerMod.mod.faceSetter.getFaceFor(summoner));
        assertNotNull(factory.getCommunicator(gm).sendCustomizeFace);
        assertThat(gm, didNotReceiveMessageContaining("Invalid"));
    }

    @Test
    void testFaceChanged() throws SQLException {
        BeastSummonerMod.mod.modelSetter.setModelFor(summoner, HUMAN_MODEL_NAME);
        long newFace = 112358;
        BeastSummonerMod.mod.faceSetter.setFaceFor(summoner, newFace + 1);

        Properties properties = new Properties();
        properties.setProperty("face", Long.toString(newFace));
        properties.setProperty("model", "human");
        properties.setProperty("confirm", "true");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(newFace, (long)BeastSummonerMod.mod.faceSetter.getFaceFor(summoner));
        assertNull(factory.getCommunicator(gm).sendCustomizeFace);
        assertThat(gm, didNotReceiveMessageContaining("Invalid"));
    }

    @Test
    void testInvalidFace() throws SQLException {
        long oldFace = 112358;
        BeastSummonerMod.mod.faceSetter.setFaceFor(summoner, oldFace);
        Properties properties = new Properties();
        properties.setProperty("face", "abc");
        properties.setProperty("confirm", "true");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(oldFace, (long)BeastSummonerMod.mod.faceSetter.getFaceFor(summoner));
        assertNull(factory.getCommunicator(gm).sendCustomizeFace);
        assertThat(gm, receivedMessageContaining("Invalid"));
    }

    @Test
    void testAsksForFaceIfModelSetHuman() throws SQLException {
        String oldModel = "old.model";
        BeastSummonerMod.mod.modelSetter.setModelFor(summoner, oldModel);
        assert BeastSummonerMod.mod.faceSetter.getFaceFor(summoner) == null;
        Properties properties = new Properties();
        properties.setProperty("model", "human");
        properties.setProperty("confirm", "true");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(HUMAN_MODEL_NAME, BeastSummonerMod.mod.modelSetter.getModelFor(summoner));
        assertNotNull(factory.getCommunicator(gm).sendCustomizeFace);
    }

    @Test
    void testModelSetTrader() throws SQLException {
        String oldModel = "old.model";
        BeastSummonerMod.mod.modelSetter.setModelFor(summoner, oldModel);
        Properties properties = new Properties();
        properties.setProperty("model", "default");
        properties.setProperty("custom_model", "blah");
        properties.setProperty("confirm", "true");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(TRADER_MODEL_NAME, BeastSummonerMod.mod.modelSetter.getModelFor(summoner));
        assertThat(gm, receivedMessageContaining(MODEL_CHANGE_SUCCESS));
        assertThat(gm, didNotReceiveMessageContaining(MODEL_CHANGE_FAILURE));
    }

    @Test
    void testModelSetHuman() throws SQLException {
        String oldModel = "old.model";
        BeastSummonerMod.mod.modelSetter.setModelFor(summoner, oldModel);
        Properties properties = new Properties();
        properties.setProperty("model", "human");
        properties.setProperty("custom_model", "blah");
        properties.setProperty("confirm", "true");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(HUMAN_MODEL_NAME, BeastSummonerMod.mod.modelSetter.getModelFor(summoner));
        assertThat(gm, receivedMessageContaining(MODEL_CHANGE_SUCCESS));
        assertThat(gm, didNotReceiveMessageContaining(MODEL_CHANGE_FAILURE));
    }

    @Test
    void testModelSetCustom() throws SQLException {
        String oldModel = "old.model";
        String customModel = "custom.model";
        BeastSummonerMod.mod.modelSetter.setModelFor(summoner, oldModel);
        Properties properties = new Properties();
        properties.setProperty("model", "custom");
        properties.setProperty("custom_model", customModel);
        properties.setProperty("confirm", "true");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(customModel, BeastSummonerMod.mod.modelSetter.getModelFor(summoner));
        assertThat(gm, receivedMessageContaining(MODEL_CHANGE_SUCCESS));
        assertThat(gm, didNotReceiveMessageContaining(MODEL_CHANGE_FAILURE));
    }

    @Test
    void testSetTag() {
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", tag);
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(tag, db.getTagFor(summoner));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testSetDifferentTag() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(summoner, tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", differentTag);
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(differentTag, db.getTagFor(summoner));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testSetTagFromDropdown() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tags", "1");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(tag, db.getTagFor(summoner));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testDropdownTagOverridesManualTag() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(factory.createNewBeastSummoner(), tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", differentTag);
        properties.setProperty("tags", "1");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(tag, db.getTagFor(summoner));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testRemoveTag() throws BeastSummonerDatabase.FailedToUpdateTagException {
        db.updateTagFor(summoner, tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", "");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals("", db.getTagFor(summoner));
        assertThat(gm, receivedMessageContaining("unique summon list"));
    }

    @Test
    void testRemoveTagChangesOptions() throws BeastSummonerDatabase.FailedToUpdateTagException, NoSuchCreatureTemplateException, SQLException {
        db.updateTagFor(summoner, tag);
        db.addOption(tag, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.PHEASANT_CID), 1, 1, Collections.emptySet());
        assert Objects.requireNonNull(db.getOptionsFor(summoner)).size() == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", "");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertThat(gm, receivedMessageContaining("unique summon list"));
        assertNull(db.getOptionsFor(summoner));
    }

    @Test
    void testSetTagChangesOptions() throws BeastSummonerDatabase.FailedToUpdateTagException, NoSuchCreatureTemplateException, SQLException {
        db.addOption(summoner, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.PHEASANT_CID), 1, 1, Collections.emptySet());
        assert Objects.requireNonNull(db.getOptionsFor(summoner)).size() == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", tag);
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertThat(gm, receivedMessageContaining("tag was set"));
        assertNull(db.getOptionsFor(summoner));
    }

    @Test
    void testSetDropdownTagChangesOptions() throws BeastSummonerDatabase.FailedToUpdateTagException, NoSuchCreatureTemplateException, SQLException {
        db.updateTagFor(factory.createNewBeastSummoner(), tag);
        db.addOption(summoner, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.PHEASANT_CID), 1, 1, Collections.emptySet());
        assert Objects.requireNonNull(db.getOptionsFor(summoner)).size() == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tags", "1");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertThat(gm, receivedMessageContaining("tag was set"));
        assertNull(db.getOptionsFor(summoner));
    }

    @Test
    void testEditTagsButtonSelected() {
        Properties properties = new Properties();
        properties.setProperty("edit", "true");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);
        new BeastSummonerEditTagsQuestion(gm).sendQuestion();

        assertThat(gm, bmlEqual());
    }

    @Test
    void testEditTagsButtonSelectedDoesNotChangeTagOrDeleteStock() throws NoSuchCreatureTemplateException, SQLException {
        db.addOption(summoner, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.PHEASANT_CID), 1, 1, Collections.emptySet());
        assert Objects.requireNonNull(db.getOptionsFor(summoner)).size() == 1;

        Properties properties = new Properties();
        properties.setProperty("edit", "true");
        properties.setProperty("tag", tag);
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);
        new BeastSummonerEditTagsQuestion(gm).sendQuestion();

        assertThat(gm, bmlEqual());
        assertEquals("", db.getTagFor(summoner));
        assertEquals(1, Objects.requireNonNull(db.getOptionsFor(summoner)).size());
    }

    @Test
    void testListButtonSelected() {
        Properties properties = new Properties();
        properties.setProperty("list", "true");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);
        new BeastSummonerSummonsListQuestion(gm, summoner).sendQuestion();

        assertThat(gm, bmlEqual());
    }

    @Test
    void testListButtonSelectedDoesNotChangeTagOrDeleteStock() throws NoSuchCreatureTemplateException, SQLException {
        db.addOption(summoner, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.PHEASANT_CID), 1, 1, Collections.emptySet());
        assert Objects.requireNonNull(db.getOptionsFor(summoner)).size() == 1;

        Properties properties = new Properties();
        properties.setProperty("list", "true");
        properties.setProperty("tag", tag);
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);
        new BeastSummonerSummonsListQuestion(gm, summoner).sendQuestion();

        assertThat(gm, bmlEqual());
        assertEquals("", db.getTagFor(summoner));
        assertEquals(1, Objects.requireNonNull(db.getOptionsFor(summoner)).size());
    }

    @Test
    void testDismissButtonSelected() throws NoSuchCreatureTemplateException, SQLException {
        db.addOption(summoner, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.PHEASANT_CID), 1, 1, Collections.emptySet());
        assert Objects.requireNonNull(db.getOptionsFor(summoner)).size() == 1;
        assert factory.getAllCreatures().size() == 3;

        Properties properties = new Properties();
        properties.setProperty("dismiss", "true");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(2, factory.getAllCreatures().size());
        assertThat(gm, receivedMessageContaining("dismiss"));
        assertNull(db.getProfileFor(summoner));
        assertNull(db.getOptionsFor(summoner));
    }

    @Test
    void testCannotDismissIfIsTrading() throws NoSuchCreatureTemplateException, SQLException {
        db.addOption(summoner, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.PHEASANT_CID), 1, 1, Collections.emptySet());
        assert Objects.requireNonNull(db.getOptionsFor(summoner)).size() == 1;
        assert factory.getAllCreatures().size() == 3;

        summoner.setTrade(new Trade(player, summoner));

        Properties properties = new Properties();
        properties.setProperty("dismiss", "true");
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(3, factory.getAllCreatures().size());
        assertThat(gm, receivedMessageContaining("is trading"));
        assertEquals(1, Objects.requireNonNull(db.getOptionsFor(summoner)).size());
    }

    @Test
    void testSetCurrency() {
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        int templateIndex = 101;
        ItemTemplatesDropdown template = new ItemTemplatesDropdown();
        int templateId = template.getTemplateOrNull(templateIndex - 1).getTemplateId();
        properties.setProperty("template", String.valueOf(templateIndex));
        new BeastSummonerManagementQuestion(gm, summoner).answer(properties);

        assertEquals(templateId, Objects.requireNonNull(db.getProfileFor(summoner)).currency.getTemplateId());
        assertThat(gm, receivedMessageContaining("currency to use"));
    }

    @Test
    void testCurrentCurrencySetProperly() throws NoSuchTemplateException, SQLException {
        db.setCurrencyFor(summoner, ItemList.sprout);
        new BeastSummonerManagementQuestion(gm, summoner).sendQuestion();

        assertThat(gm, receivedBMLContaining("default=\"" + new ItemTemplatesDropdown().getIndexOf(Objects.requireNonNull(db.getProfileFor(summoner)).currency) + "\""));
    }
}
