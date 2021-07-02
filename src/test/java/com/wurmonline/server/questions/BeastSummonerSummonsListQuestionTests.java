package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.CreatureTemplateFactory;
import com.wurmonline.server.creatures.CreatureTemplateIds;
import com.wurmonline.server.creatures.NoSuchCreatureTemplateException;
import mod.wurmunlimited.npcs.beastsummoner.SummonOption;
import mod.wurmunlimited.npcs.beastsummoner.db.BeastSummonerDatabase;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BeastSummonerSummonsListQuestionTests extends BeastSummonerListTest {
    private BeastSummonerSummonsListQuestion question;

    private void setNextQuestion() {
        try {
            question = (BeastSummonerSummonsListQuestion)Questions.getQuestion(question.id + 1);
        } catch (NoSuchQuestionException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean gmDidNotReceive(String template) {
        String last = factory.getCommunicator(gm).lastBmlContent;
        Pattern pattern = Pattern.compile(getRegex(template));
        return !pattern.matcher(last).find();
    }

    @Test
    void testOptionsAddedToList() throws SQLException {
        int n = 5;
        CreatureTemplate template1 = getRandomTemplate();
        CreatureTemplate template2 = getRandomTemplate();
        db.addOption(summoner, template1, n, n + 1, Collections.emptySet());
        db.addOption(summoner, template2, n + 1, n + 2, Collections.emptySet());
        new BeastSummonerSummonsListQuestion(gm, summoner).sendQuestion();

        assertThat(gm, receivedBMLContaining("label{text=\"" + template1.getName() + "\"};" +
                                                     "label{text=\"" + n + "i\"};" +
                                                     "label{text=\"" + (n + 1) + "\"};"));
        assertThat(gm, receivedBMLContaining("label{text=\"" + template2.getName() + "\"};" +
                                                     "label{text=\"" + (n + 1) + "i\"};" +
                                                     "label{text=\"" + (n + 2) + "\"};"));
    }

    @Test
    void testSubmitListDoesNothing() throws SQLException {
        int n = 5;
        db.addOption(summoner, getRandomTemplate(), n, n + 1, Collections.emptySet());
        db.addOption(summoner, getRandomTemplate(), n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        properties.setProperty("r0", "true");
        properties.setProperty("e0", "true");
        properties.setProperty("confirm", "true");
        new BeastSummonerSummonsListQuestion(gm, summoner).answer(properties);

        List<SummonOption> options = db.getOptionsFor(summoner);
        assertEquals(2, Objects.requireNonNull(options).size());
        assertEquals(oldOptions.get(0), options.get(0));
        assertEquals(oldOptions.get(1), options.get(1));
    }

    @Test
    void testAddListSendsDifferentQuestion() throws SQLException {
        int n = 5;
        db.addOption(summoner, getRandomTemplate(), n, n + 1, Collections.emptySet());
        db.addOption(summoner, getRandomTemplate(), n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        properties.setProperty("r0", "true");
        properties.setProperty("e0", "true");
        properties.setProperty("add", "true");
        question = new BeastSummonerSummonsListQuestion(gm, summoner);
        question.sendQuestion();
        question.answer(properties);
        setNextQuestion();

        assertThat(gm, bmlNotEqual());
        assertThat(gm, receivedBMLContaining("Creature type modifier"));
        List<SummonOption> options = db.getOptionsFor(summoner);
        assertEquals(2, Objects.requireNonNull(options).size());
        assertEquals(oldOptions.get(0), options.get(0));
        assertEquals(oldOptions.get(1), options.get(1));
    }

    @Test
    void testRemoveOption() throws SQLException {
        int n = 5;
        db.addOption(summoner, getRandomTemplate(), n, n + 1, Collections.emptySet());
        db.addOption(summoner, getRandomTemplate(), n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        properties.setProperty("remove", "true");
        properties.setProperty("r0", "true");
        new BeastSummonerSummonsListQuestion(gm, summoner).answer(properties);

        List<SummonOption> options = db.getOptionsFor(summoner);
        assertEquals(1, Objects.requireNonNull(options).size());
        assertEquals(oldOptions.get(1), options.get(0));
    }

    @Test
    void testEditListSendsDifferentQuestion() throws SQLException {
        int n = 5;
        db.addOption(summoner, getRandomTemplate(), n, n + 1, Collections.emptySet());
        db.addOption(summoner, getRandomTemplate(), n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        properties.setProperty("edit", "true");
        properties.setProperty("e0", "true");
        question = new BeastSummonerSummonsListQuestion(gm, summoner);
        question.sendQuestion();
        question.answer(properties);
        setNextQuestion();

        assertThat(gm, bmlNotEqual());
        assertThat(gm, receivedBMLContaining("default=\"" + new CreatureTemplatesDropdown(Collections.singletonList(oldOptions.get(1).template)).getIndexOf(oldOptions.get(0).template)));
        assertThat(gm, receivedBMLContaining("Creature type modifier"));
        List<SummonOption> options = db.getOptionsFor(summoner);
        assertEquals(2, Objects.requireNonNull(options).size());
        assertEquals(oldOptions.get(0), options.get(0));
        assertEquals(oldOptions.get(1), options.get(1));
    }

    @Test
    void testNothingSelectedResendsQuestion() throws SQLException {
        int n = 5;
        db.addOption(summoner, getRandomTemplate(), n, n + 1, Collections.emptySet());
        db.addOption(summoner, getRandomTemplate(), n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        question = new BeastSummonerSummonsListQuestion(gm, summoner);
        question.sendQuestion();
        question.answer(properties);
        setNextQuestion();

        assertThat(gm, bmlEqual());
        List<SummonOption> options = db.getOptionsFor(summoner);
        assertEquals(2, Objects.requireNonNull(options).size());
        assertEquals(oldOptions.get(0), options.get(0));
        assertEquals(oldOptions.get(1), options.get(1));
    }

    @Test
    void testAddHasUsedTemplateRemoved() throws SQLException {
        int n = 5;
        CreatureTemplate template1 = getRandomTemplate();
        CreatureTemplate template2 = getRandomTemplate();
        db.addOption(summoner, template1, n, n + 1, Collections.emptySet());
        db.addOption(summoner, template2, n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        properties.setProperty("add", "true");
        new BeastSummonerSummonsListQuestion(gm, summoner).answer(properties);

        String last = factory.getCommunicator(gm).lastBmlContent;
        assertFalse(Pattern.compile(getRegex(template1.getName())).matcher(last).find());
        assertFalse(Pattern.compile(getRegex(template2.getName())).matcher(last).find());
    }

    @Test
    void testFilterAdd() throws SQLException, NoSuchCreatureTemplateException {
        int n = 5;
        CreatureTemplate template1 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.CALF_CID);
        db.addOption(summoner, template1, n, n + 1, Collections.emptySet());
        db.addOption(summoner, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.CRAB_CID), n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        properties.setProperty("add", "true");
        question = new BeastSummonerSummonsListQuestion(gm, summoner);
        question.answer(properties);
        setNextQuestion();
        assert gmDidNotReceive(template1.getName());
        properties.setProperty("do_filter", "true");
        properties.setProperty("filter", "Rift*");
        question.answer(properties);
        setNextQuestion();

        String all = "options=\"" + String.join(",", "Rift Beast", "Rift Caster", "Rift Jackal", "Rift Ogre", "Rift Ogre Mage", "Rift Summoner", "Rift Warmaster") + "\"";

        assertThat(gm, receivedBMLContaining(all));
    }

    @Test
    void testFilterEdit() throws SQLException, NoSuchCreatureTemplateException {
        int n = 5;
        CreatureTemplate template1 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.SEAL_CID);
        db.addOption(summoner, template1, n, n + 1, Collections.emptySet());
        db.addOption(summoner, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.SEA_SERPENT_CID), n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        properties.setProperty("edit", "true");
        properties.setProperty("e0", "true");
        question = new BeastSummonerSummonsListQuestion(gm, summoner);
        question.answer(properties);
        setNextQuestion();
        assert !gmDidNotReceive(template1.getName());
        properties.setProperty("do_filter", "true");
        properties.setProperty("filter", "Rift*");
        question.answer(properties);
        setNextQuestion();

        String all = "options=\"" + String.join(",", "Rift Beast", "Rift Caster", "Rift Jackal", "Rift Ogre", "Rift Ogre Mage", "Rift Summoner", "Rift Warmaster") + "\"";

        assertThat(gm, receivedBMLContaining(all));
    }

    @Test
    void testSubmitAdd() throws SQLException {
        int n = 5;
        int price = 12;
        int cap = 34;
        CreatureTemplate template1 = getRandomTemplate();
        db.addOption(summoner, template1, n, n + 1, Collections.emptySet());
        db.addOption(summoner, getRandomTemplate(), n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        properties.setProperty("add", "true");
        question = new BeastSummonerSummonsListQuestion(gm, summoner);
        question.answer(properties);
        setNextQuestion();
        assert gmDidNotReceive(template1.getName());
        properties.setProperty("confirm", "true");
        properties.setProperty("template", "0");
        properties.setProperty("price", String.valueOf(price));
        properties.setProperty("cap", String.valueOf(cap));
        question.answer(properties);
        setNextQuestion();

        List<SummonOption> newOptions = db.getOptionsFor(summoner);
        assertEquals(3, Objects.requireNonNull(newOptions).size());
        SummonOption option = newOptions.get(2);
        assertEquals(new CreatureTemplatesDropdown(oldOptions.stream().map(it -> it.template).collect(Collectors.toList())).getTemplateOrNull(0), option.template);
        assertEquals(price, option.price);
        assertEquals(cap, option.cap);
    }

    @Test
    void testSubmitEditSameTemplate() throws SQLException {
        int n = 5;
        int price = 56;
        int cap = 78;
        CreatureTemplate template1 = getRandomTemplate();
        CreatureTemplate template2 = getRandomTemplate();
        db.addOption(summoner, template1, n, n + 1, Collections.emptySet());
        db.addOption(summoner, template2, n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;
        int templateIndex = new CreatureTemplatesDropdown(Collections.singletonList(template2)).getIndexOf(template1);

        Properties properties = new Properties();
        properties.setProperty("edit", "true");
        properties.setProperty("e0", "true");
        question = new BeastSummonerSummonsListQuestion(gm, summoner);
        question.answer(properties);
        setNextQuestion();
        assert !gmDidNotReceive(template1.getName());
        properties.setProperty("confirm", "true");
        properties.setProperty("template", String.valueOf(templateIndex));
        properties.setProperty("price", String.valueOf(price));
        properties.setProperty("cap", String.valueOf(cap));
        question.answer(properties);
        setNextQuestion();

        List<SummonOption> newOptions = db.getOptionsFor(summoner);
        assertEquals(2, Objects.requireNonNull(newOptions).size());
        SummonOption option = newOptions.get(0);
        assertEquals(template1, option.template);
        assertEquals(price, option.price);
        assertEquals(cap, option.cap);
    }

    @Test
    void testSubmitEditDifferentTemplate() throws SQLException {
        int n = 5;
        int price = 56;
        int cap = 78;
        CreatureTemplate template1 = getRandomTemplate();
        CreatureTemplate template2 = getRandomTemplate();
        db.addOption(summoner, template1, n, n + 1, Collections.emptySet());
        db.addOption(summoner, template2, n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;
        int templateIndex = 0;
        if (new CreatureTemplatesDropdown(Collections.singletonList(template2)).getTemplateOrNull(0) == template1) {
            templateIndex = 1;
        }

        Properties properties = new Properties();
        properties.setProperty("edit", "true");
        properties.setProperty("e0", "true");
        question = new BeastSummonerSummonsListQuestion(gm, summoner);
        question.answer(properties);
        setNextQuestion();
        assert !gmDidNotReceive(template1.getName());
        properties.setProperty("confirm", "true");
        properties.setProperty("template", String.valueOf(templateIndex));
        properties.setProperty("price", String.valueOf(price));
        properties.setProperty("cap", String.valueOf(cap));
        question.answer(properties);
        setNextQuestion();

        List<SummonOption> newOptions = db.getOptionsFor(summoner);
        assertEquals(2, Objects.requireNonNull(newOptions).size());
        SummonOption option = newOptions.get(0);
        assertEquals(new CreatureTemplatesDropdown(Collections.singletonList(template2)).getTemplateOrNull(templateIndex), option.template);
        assertEquals(price, option.price);
        assertEquals(cap, option.cap);
    }

    @Test
    void testPriceMustBeGreaterThanZero() throws SQLException {
        int n = 5;
        int cap = 34;
        CreatureTemplate template1 = getRandomTemplate();
        db.addOption(summoner, template1, n, n + 1, Collections.emptySet());
        db.addOption(summoner, getRandomTemplate(), n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        properties.setProperty("add", "true");
        question = new BeastSummonerSummonsListQuestion(gm, summoner);
        question.answer(properties);
        setNextQuestion();
        assert gmDidNotReceive(template1.getName());
        properties.setProperty("confirm", "true");
        properties.setProperty("template", "0");
        properties.setProperty("price", "0");
        properties.setProperty("cap", String.valueOf(cap));
        question.answer(properties);
        setNextQuestion();

        List<SummonOption> newOptions = db.getOptionsFor(summoner);
        assertEquals(3, Objects.requireNonNull(newOptions).size());
        SummonOption option = newOptions.get(2);
        assertEquals(1, option.price);
        assertEquals(cap, option.cap);
    }

    @Test
    void testCapMustBeGreaterThanZero() throws SQLException {
        int n = 5;
        int price = 34;
        CreatureTemplate template1 = getRandomTemplate();
        db.addOption(summoner, template1, n, n + 1, Collections.emptySet());
        db.addOption(summoner, getRandomTemplate(), n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        properties.setProperty("add", "true");
        question = new BeastSummonerSummonsListQuestion(gm, summoner);
        question.answer(properties);
        setNextQuestion();
        assert gmDidNotReceive(template1.getName());
        properties.setProperty("confirm", "true");
        properties.setProperty("template", "0");
        properties.setProperty("price", String.valueOf(price));
        properties.setProperty("cap", "0");
        question.answer(properties);
        setNextQuestion();

        List<SummonOption> newOptions = db.getOptionsFor(summoner);
        assertEquals(3, Objects.requireNonNull(newOptions).size());
        SummonOption option = newOptions.get(2);
        assertEquals(price, option.price);
        assertEquals(1, option.cap);
    }


    // Tags

    @Test
    void testSubmitAddTags() throws SQLException, BeastSummonerDatabase.FailedToUpdateTagException {
        int n = 5;
        int price = 12;
        int cap = 34;
        String tag = "myTag";
        CreatureTemplate template1 = getRandomTemplate();
        db.updateTagFor(summoner, tag);
        db.addOption(tag, template1, n, n + 1, Collections.emptySet());
        db.addOption(tag, getRandomTemplate(), n + 1, n + 2, Collections.emptySet());
        List<SummonOption> oldOptions = new ArrayList<>(Objects.requireNonNull(db.getOptionsFor(summoner)));
        assert oldOptions.size() == 2;

        Properties properties = new Properties();
        properties.setProperty("add", "true");
        question = new BeastSummonerSummonsListQuestion(gm, summoner);
        question.answer(properties);
        setNextQuestion();
        assert gmDidNotReceive(template1.getName());
        properties.setProperty("confirm", "true");
        properties.setProperty("template", "0");
        properties.setProperty("price", String.valueOf(price));
        properties.setProperty("cap", String.valueOf(cap));
        question.answer(properties);
        setNextQuestion();

        List<SummonOption> newOptions = db.getOptionsFor(summoner);
        assertEquals(3, Objects.requireNonNull(newOptions).size());
        SummonOption option = newOptions.get(2);
        assertEquals(new CreatureTemplatesDropdown(oldOptions.stream().map(it -> it.template).collect(Collectors.toList())).getTemplateOrNull(0), option.template);
        assertEquals(price, option.price);
        assertEquals(cap, option.cap);
    }
}
