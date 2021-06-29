package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.*;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Trade;
import mod.wurmunlimited.npcs.beastsummoner.SummonOption;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class BeastSummonerRequestQuestionTests extends BeastSummonerListTest {
    private void createOptions(CreatureTemplate template1, CreatureTemplate template2, int n) {
        try {
            db.addOption(summoner, template1, n, n + 1, Collections.emptySet());
            db.addOption(summoner, template2, n + 1, n + 2, Collections.emptySet());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getFullName(SummonOption option, byte age, byte type) {
        return getFullName(option, 1, age, type);
    }

    private String getFullName(SummonOption option, int amount, byte age, byte type) {
        return new SummonRequest.SummonRequestDetails(option, type, age, amount).nameWithoutAmount;
    }

    private String getFullNameWithAmountBML(SummonOption option, int amount) {
        return getFullName(option, amount, (byte)0, (byte)0) + "\"};label{text=\"" + amount + "\"}";
    }

    private void add(BeastSummonerRequestQuestion question) {
        Properties properties = new Properties();
        properties.setProperty("add", "true");
        question.answer(properties);
    }

    private void remove(BeastSummonerRequestQuestion question, int index) {
        Properties properties = new Properties();
        properties.setProperty("r" + index, "true");
        question.answer(properties);
    }

    private void back(BeastSummonerRequestQuestion question) {
        Properties properties = new Properties();
        properties.setProperty("back", "true");
        question.answer(properties);
    }

    private void submit(BeastSummonerRequestQuestion question) {
        Properties properties = new Properties();
        properties.setProperty("submit", "true");
        question.answer(properties);
    }

    private void selectOption(BeastSummonerRequestQuestion question, int index) {
        Properties properties = new Properties();
        properties.setProperty("a" + index, "true");
        question.answer(properties);
    }

    // Relative to getOptionsFor.
    private void selectFirstOption(BeastSummonerRequestQuestion question) {
        List<SummonOption> options = db.getOptionsFor(summoner);
        assert options != null;
        List<SummonOption> sorted = new ArrayList<>(options);
        sorted.sort(Comparator.comparing(it -> it.template.getName()));
        int index = sorted.indexOf(options.get(0));
        assert index != -1;
        selectOption(question, index);
    }

    private void setOptionDetails(BeastSummonerRequestQuestion question, int amount, byte age, byte type) {
        Properties properties = new Properties();
        properties.setProperty("amount", String.valueOf(amount));
        properties.setProperty("age", String.valueOf(age));
        properties.setProperty("type", String.valueOf(type));
        question.answer(properties);
    }

    @Test
    void testSubmitListDoesNothing() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();

        submit(new BeastSummonerRequestQuestion(player, summoner));

        assertNull(player.getTrade());
        assertNull(summoner.getTrade());
        assertEquals(currentCreatures, factory.getAllCreatures().size());
        assertThat(player, receivedMessageContaining("You decide not"));
    }

    @Test
    void testOptionsAddedToList() {
        int n = 5;
        CreatureTemplate template1 = getRandomTemplate();
        CreatureTemplate template2 = getRandomTemplate();
        createOptions(template1, template2, n);

        add(new BeastSummonerRequestQuestion(player, summoner));

        assertThat(player, receivedBMLContaining("label{text=\"" + template1.getName() + "\"};" +
                                                         "label{text=\"" + n + "i\"};" +
                                                         "label{text=\"" + (n + 1) + "\"};" +
                                                         "label{text=\"None\"};"));
        assertThat(player, receivedBMLContaining("label{text=\"" + template2.getName() + "\"};" +
                                                         "label{text=\"" + (n + 1) + "i\"};" +
                                                         "label{text=\"" + (n + 2) + "\"};" +
                                                         "label{text=\"None\"};"));
    }

    @Test
    void testOptionsAreRemoved() {
        int n = 5;
        CreatureTemplate template1 = getRandomTemplate();
        CreatureTemplate template2 = getRandomTemplate();
        createOptions(template1, template2, n);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        int amount = 3;
        byte age = 45;
        setOptionDetails(question, amount, age, (byte)0);
        factory.getCommunicator(player).clearBml();
        add(question);

        assertThat(player, didNotReceiveBMLContaining("label{text=\"" + template1.getName() + "\"};" +
                                                              "label{text=\"" + n + "i\"};" +
                                                              "label{text=\"" + (n + 1) + "\"};" +
                                                              "label{text=\"None\"};"));
        assertThat(player, receivedBMLContaining("label{text=\"" + template2.getName() + "\"};" +
                                                         "label{text=\"" + (n + 1) + "i\"};" +
                                                         "label{text=\"" + (n + 2) + "\"};" +
                                                         "label{text=\"None\"};"));
    }

    @Test
    void testOptionsAreReAddedWhenRemoved() {
        int n = 5;
        CreatureTemplate template1 = getRandomTemplate();
        CreatureTemplate template2 = getRandomTemplate();
        createOptions(template1, template2, n);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        int amount = 3;
        byte age = 45;
        setOptionDetails(question, amount, age, (byte)0);
        factory.getCommunicator(player).clearBml();
        add(question);

        assertThat(player, didNotReceiveBMLContaining("label{text=\"" + template1.getName() + "\"};" +
                                                              "label{text=\"" + n + "i\"};" +
                                                              "label{text=\"" + (n + 1) + "\"};" +
                                                              "label{text=\"None\"};"));
        assertThat(player, receivedBMLContaining("label{text=\"" + template2.getName() + "\"};" +
                                                         "label{text=\"" + (n + 1) + "i\"};" +
                                                         "label{text=\"" + (n + 2) + "\"};" +
                                                         "label{text=\"None\"};"));


        back(question);
        remove(question, 0);
        factory.getCommunicator(player).clearBml();
        add(question);

        assertThat(player, receivedBMLContaining("label{text=\"" + template1.getName() + "\"};" +
                                                         "label{text=\"" + n + "i\"};" +
                                                         "label{text=\"" + (n + 1) + "\"};" +
                                                         "label{text=\"None\"};"));
        assertThat(player, receivedBMLContaining("label{text=\"" + template2.getName() + "\"};" +
                                                         "label{text=\"" + (n + 1) + "i\"};" +
                                                         "label{text=\"" + (n + 2) + "\"};" +
                                                         "label{text=\"None\"};"));
    }

    @Test
    void testAddAddRemoveLastReAdded() {
        int n = 5;
        CreatureTemplate template1 = getRandomTemplate();
        CreatureTemplate template2 = getRandomTemplate();
        createOptions(template1, template2, n);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        int amount = 3;
        byte age = 45;
        setOptionDetails(question, amount, age, (byte)0);
        add(question);
        selectOption(question, 0);
        setOptionDetails(question, amount, age, (byte)0);
        factory.getCommunicator(player).clearBml();
        add(question);

        assertThat(player, didNotReceiveBMLContaining("label{text=\"" + template1.getName() + "\"};" +
                                                              "label{text=\"" + n + "i\"};" +
                                                              "label{text=\"" + (n + 1) + "\"};" +
                                                              "label{text=\"None\"};"));
        assertThat(player, didNotReceiveBMLContaining("label{text=\"" + template2.getName() + "\"};" +
                                                              "label{text=\"" + (n + 1) + "i\"};" +
                                                              "label{text=\"" + (n + 2) + "\"};" +
                                                              "label{text=\"None\"};"));


        back(question);
        remove(question, 1);
        factory.getCommunicator(player).clearBml();
        add(question);

        assertThat(player, didNotReceiveBMLContaining("label{text=\"" + template1.getName() + "\"};" +
                                                              "label{text=\"" + n + "i\"};" +
                                                              "label{text=\"" + (n + 1) + "\"};" +
                                                              "label{text=\"None\"};"));
        assertThat(player, receivedBMLContaining("label{text=\"" + template2.getName() + "\"};" +
                                                              "label{text=\"" + (n + 1) + "i\"};" +
                                                              "label{text=\"" + (n + 2) + "\"};" +
                                                              "label{text=\"None\"};"));
    }

    @Test
    void testRemovedReAddedSubmitDoesNothing() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        int amount = 3;
        byte age = 45;
        setOptionDetails(question, amount, age, (byte)0);
        remove(question, 0);
        submit(question);

        assertNull(player.getTrade());
        assertNull(summoner.getTrade());
        assertEquals(currentCreatures, factory.getAllCreatures().size());
        assertThat(player, receivedMessageContaining("You decide not"));
    }

    @Test
    void testPriceTotalIsCorrect() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        int amount = 3;
        byte age = 45;
        setOptionDetails(question, amount, age, (byte)0);

        assertThat(player, receivedBMLContaining("Current total - " + 15 + "i"));
    }

    @Test
    void testPriceTotalIsCorrectMultiple() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        int amount = 3;
        byte age = 45;
        setOptionDetails(question, amount, age, (byte)0);
        add(question);
        selectOption(question, 0);
        setOptionDetails(question, amount, age, (byte)0);

        assertThat(player, receivedBMLContaining("Current total - " + (5 * 3 + 6 * 3) + "i"));
    }

    @Test
    void testTypeColumnNone() throws SQLException {
        db.addOption(summoner, getRandomTemplate(), 1, 2, Collections.emptySet());

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);

        assertThat(player, receivedBMLContaining("label{text=\"2\"};label{text=\"None\"}"));
    }

    @Test
    void testTypeColumnOne() throws SQLException {
        db.addOption(summoner, getRandomTemplate(), 1, 2, Collections.singleton((byte)5));

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);

        assertThat(player, receivedBMLContaining("label{text=\"2\"};label{text=\"Alert\"}"));
    }

    @Test
    void testTypeColumnNotAll() throws SQLException {
        Set<Byte> types = new HashSet<>(CreatureTypeList.all);
        types.remove((byte)5);
        db.addOption(summoner, getRandomTemplate(), 1, 2, types);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);

        assertThat(player, receivedBMLContaining("label{text=\"2\"};label{text=\"Some\"}"));
    }

    @Test
    void testTypeColumnAll() throws SQLException {
        db.addOption(summoner, getRandomTemplate(), 1, 2, new HashSet<>(CreatureTypeList.all));

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);

        assertThat(player, receivedBMLContaining("label{text=\"2\"};label{text=\"Any\"}"));
    }

    @Test
    void testCorrectTypesAvailableNone() throws SQLException {
        db.addOption(summoner, getRandomTemplate(), 1, 2, Collections.emptySet());
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);

        Pattern pattern = Pattern.compile("radio\\{group=\"type\";id=\"0\";text=\"No modifier\";selected=\"true\"}}};null;null;}$");
        assertTrue(pattern.matcher(factory.getCommunicator(player).lastBmlContent).find(),
                pattern + " / " + factory.getCommunicator(player).lastBmlContent);
    }

    @Test
    void testCorrectTypesAvailableOneNotNone() throws SQLException {
        db.addOption(summoner, getRandomTemplate(), 1, 2, Collections.singleton((byte)5));
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);

        Pattern pattern = Pattern.compile("radio\\{group=\"type\";id=\"5\";text=\"Alert\";selected=\"true\"}}};null;null;}$");
        assertTrue(pattern.matcher(factory.getCommunicator(player).lastBmlContent).find(),
                pattern + " / " + factory.getCommunicator(player).lastBmlContent);
    }

    @Test
    void testCorrectTypesAvailableAll() throws SQLException {
        db.addOption(summoner, getRandomTemplate(), 1, 2, new HashSet<>(CreatureTypeList.all));
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);

        List<String> entries = new ArrayList<>();
        for (Map.Entry<Byte, String> entry : CreatureTypeList.creatureTypes) {
            entries.add("radio\\{group=\"type\";id=\"" + entry.getKey() + "\";text=\"" + entry.getValue() + "\"" +
                                (entry.getKey() == 0 ? ";selected=\"true\"" : "") +
                                "}");
        }
        Pattern pattern = Pattern.compile(String.join("", entries) + "}};null;null;}$");
        assertTrue(pattern.matcher(factory.getCommunicator(player).lastBmlContent).find(),
                pattern + " / " + factory.getCommunicator(player).lastBmlContent);
    }

    @Test
    void testAddOptionDetails() {
        CreatureTemplate template1 = getRandomTemplate();
        createOptions(template1, getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);

        assertThat(player, receivedBMLContaining("Details for - " + template1.getName()));
        assertNull(player.getTrade());
        assertNull(summoner.getTrade());
        assertEquals(currentCreatures, factory.getAllCreatures().size());
        assertThat(player, didNotReceiveMessageContaining("You decide not"));
    }

    @Test
    void testAddBackDoesNothing() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        back(question);
        submit(question);

        assertNull(player.getTrade());
        assertNull(summoner.getTrade());
        assertEquals(currentCreatures, factory.getAllCreatures().size());
        assertThat(player, receivedMessageContaining("You decide not"));
    }

    @Test
    void testAddBadInput() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        Properties properties = new Properties();
        properties.setProperty("a123", "true");
        factory.getCommunicator(player).clearBml();
        question.answer(properties);
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();

        assertThat(player, bmlEqual());
        assertNull(player.getTrade());
        assertNull(summoner.getTrade());
        assertEquals(currentCreatures, factory.getAllCreatures().size());
        assertThat(player, receivedMessageContaining("I do not understand"));
    }

    @Test
    void testAddSecondOptionDetails() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).stream().sorted(Comparator.comparing(it -> it.template.getName())).collect(Collectors.toList()).get(1);
        int currentCreatures = factory.getAllCreatures().size();

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectOption(question, 1);

        assertThat(player, receivedBMLContaining("Details for - " + option.template.getName()));
        assertNull(player.getTrade());
        assertNull(summoner.getTrade());
        assertEquals(currentCreatures, factory.getAllCreatures().size());
        assertThat(player, didNotReceiveMessageContaining("You decide not"));
    }

    @Test
    void testAddToDetailsToList() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);
        int currentCreatures = factory.getAllCreatures().size();

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        int amount = 3;
        byte age = 45;
        setOptionDetails(question, amount, age, (byte)0);

        assertThat(player, receivedBMLContaining(getFullName(option, age, (byte)0)));
        assertNull(player.getTrade());
        assertNull(summoner.getTrade());
        assertEquals(currentCreatures, factory.getAllCreatures().size());
        assertThat(player, didNotReceiveMessageContaining("You decide not"));
    }

    @Test
    void testAddToDetailsCancel() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        Properties properties = new Properties();
        properties.setProperty("cancel", "true");
        factory.getCommunicator(player).clearBml();
        question.answer(properties);
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();

        assertThat(player, bmlEqual());
        assertNull(player.getTrade());
        assertNull(summoner.getTrade());
        assertEquals(currentCreatures, factory.getAllCreatures().size());
        assertThat(player, didNotReceiveMessageContaining("You decide not"));
    }

    @Test
    void testAddToDetailsNegativeAmount() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        factory.getCommunicator(player).clearBml();
        setOptionDetails(question, -1, (byte)0, (byte)0);

        assertThat(player, receivedBMLContaining(getFullNameWithAmountBML(option, 1)));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlNotEqual());
        assertThat(player, receivedMessageContaining("must be 1 or greater"));
    }

    @Test
    void testAddToDetailsBadAmount() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        factory.getCommunicator(player).clearBml();
        Properties properties = new Properties();
        properties.setProperty("amount", "abc");
        properties.setProperty("age", "0");
        properties.setProperty("type", "0");
        question.answer(properties);

        assertThat(player, receivedBMLContaining(getFullNameWithAmountBML(option, 1)));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlNotEqual());
        assertThat(player, receivedMessageContaining("didn't understand how many"));
    }

    @Test
    void testAddToDetailsAmountAboveCap() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        factory.getCommunicator(player).clearBml();
        setOptionDetails(question, option.cap + 1, (byte)0, (byte)0);

        assertThat(player, receivedBMLContaining(getFullNameWithAmountBML(option, option.cap)));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlNotEqual());
        assertThat(player, receivedMessageContaining("cannot summon that many"));
    }

    @Test
    void testAddToDetailsNegativeAge() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        factory.getCommunicator(player).clearBml();
        setOptionDetails(question, 1, (byte)-1, (byte)0);

        assertThat(player, receivedBMLContaining(getFullName(option, (byte)0, (byte)0)));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlNotEqual());
        assertThat(player, receivedMessageContaining("must be 0 or greater"));
    }

    @Test
    void testAddToDetailsBadAge() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        factory.getCommunicator(player).clearBml();
        Properties properties = new Properties();
        properties.setProperty("amount", "1");
        properties.setProperty("age", "abc");
        properties.setProperty("type", "0");
        question.answer(properties);

        assertThat(player, receivedBMLContaining(getFullName(option, (byte)0, (byte)0)));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlNotEqual());
        assertThat(player, receivedMessageContaining("didn't understand the age"));
    }

    @Test
    void testAddToDetailsAge1() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        factory.getCommunicator(player).clearBml();
        setOptionDetails(question, 1, (byte)1, (byte)0);

        assertThat(player, receivedBMLContaining(getFullName(option, (byte)2, (byte)0)));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlNotEqual());
        assertThat(player, receivedMessageContaining("must be 0 or greater than 2"));
    }

    @Test
    void testAddToDetailsAgeAbove100() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        factory.getCommunicator(player).clearBml();
        setOptionDetails(question, 1, (byte)101, (byte)0);

        assertThat(player, receivedBMLContaining(getFullName(option, (byte)100, (byte)0)));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlNotEqual());
        assertThat(player, receivedMessageContaining("must be 100 or lower"));
    }

    @Test
    void testAddToDetailsGoodType() throws SQLException {
        int n = 5;
        db.addOption(summoner, getRandomTemplate(), n, n + 1, new HashSet<>(CreatureTypeList.all));
        db.addOption(summoner, getRandomTemplate(), n + 1, n + 2, new HashSet<>(CreatureTypeList.all));
        int currentCreatures = factory.getAllCreatures().size();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        factory.getCommunicator(player).clearBml();
        Properties properties = new Properties();
        properties.setProperty("amount", "5");
        properties.setProperty("age", "0");
        properties.setProperty("type", "5");
        question.answer(properties);

        assertThat(player, receivedBMLContaining(getFullName(option, (byte)0, (byte)5)));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlNotEqual());
        assertThat(player, didNotReceiveMessageContaining("didn't understand the creature type"));
    }

    @Test
    void testAddToDetailsBadType() {
        createOptions(getRandomTemplate(), getRandomTemplate(), 5);
        int currentCreatures = factory.getAllCreatures().size();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        factory.getCommunicator(player).clearBml();
        Properties properties = new Properties();
        properties.setProperty("amount", "1");
        properties.setProperty("age", "0");
        properties.setProperty("type", "abc");
        question.answer(properties);

        assertThat(player, receivedBMLContaining(getFullName(option, (byte)0, (byte)0)));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlNotEqual());
        assertThat(player, receivedMessageContaining("didn't understand the creature type"));
    }

    @Test
    void testAddToDetailsTypeInRangeButNotValid() throws SQLException {
        int n = 5;
        db.addOption(summoner, getRandomTemplate(), n, n + 1, new HashSet<>(CreatureTypeList.all));
        db.addOption(summoner, getRandomTemplate(), n + 1, n + 2, new HashSet<>(CreatureTypeList.all));
        int currentCreatures = factory.getAllCreatures().size();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);

        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, summoner);
        add(question);
        selectFirstOption(question);
        factory.getCommunicator(player).clearBml();
        setOptionDetails(question, 1, (byte)0, (byte)12);

        assertThat(player, receivedBMLContaining(getFullName(option, (byte)0, (byte)0)));
        new BeastSummonerRequestQuestion(player, summoner).sendQuestion();
        assertThat(player, bmlNotEqual());
        assertThat(player, receivedMessageContaining("didn't understand the creature type"));
    }

    private Creature getSpy() {
        SummonerProfile profile = db.getProfileFor(summoner);
        assert profile != null;
        Creature spy = spy(summoner);

        try {
            db.addNew(spy, profile.spawnPointCentre, profile.floorLevel, profile.range, "");
            // Needs to be not unique for checking age in testAddThroughToSummon.
            db.addOption(spy, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.HORSE_CID), 5, 6, Collections.emptySet());
            db.addOption(spy, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.BEAR_BLACK_CID), 7, 8, Collections.emptySet());
        } catch (SQLException | NoSuchCreatureTemplateException e) {
            throw new RuntimeException(e);
        }

        AtomicReference<TradeHandler> handler = new AtomicReference<>(null);
        doAnswer((Answer<TradeHandler>)i -> {
            if (handler.get() == null) {
                handler.set(new BeastSummonerTradeHandlerCoins(summoner, summoner.getTrade()));
            }
            return handler.get();
        }).when(spy).getTradeHandler();
        return spy;
    }

    @Test
    void testAddThroughToTrade() {
        Creature spy = getSpy();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(spy)).stream().sorted(Comparator.comparing(it -> it.template.getName())).collect(Collectors.toList()).get(0);
        int currentCreatures = factory.getAllCreatures().size();
        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, spy);
        add(question);
        selectOption(question, 0);
        int amount = 3;
        byte age = 45;
        setOptionDetails(question, amount, age, (byte)0);
        submit(question);

        assertNotNull(player.getTrade());
        assertNotNull(summoner.getTrade());
        assertEquals(getFullName(option, age, (byte)0) + " x " + amount, player.getTrade().getTradingWindow(3).getItems()[0].getName());
        assertEquals(currentCreatures, factory.getAllCreatures().size());
        assertThat(player, didNotReceiveMessageContaining("You decide not"));
    }

    @Test
    void testAddThroughToSummon() {
        int n = 5;
        Creature spy = getSpy();
        int currentCreatures = factory.getAllCreatures().size();
        SummonOption option = Objects.requireNonNull(db.getOptionsFor(spy)).stream().sorted(Comparator.comparing(it -> it.template.getName())).collect(Collectors.toList()).get(0);
        BeastSummonerRequestQuestion question = new BeastSummonerRequestQuestion(player, spy);
        add(question);
        selectOption(question, 0);
        int amount = 3;
        byte age = 45;
        setOptionDetails(question, amount, age, (byte)0);
        submit(question);

        Trade trade = player.getTrade();
        assertNotNull(trade);
        assertEquals(getFullName(option, amount, age, (byte)0) + " x " + amount, trade.getTradingWindow(3).getItems()[0].getName());
        for (Item coin : Economy.getEconomy().getCoinsFor(n * amount)) {
            player.getInventory().insertItem(coin);
            trade.getTradingWindow(4).addItem(coin);
        }
        trade.setSatisfied(player, true, trade.getCurrentCounter());
        trade.setSatisfied(summoner, true, trade.getCurrentCounter());
        assertEquals(currentCreatures + amount, factory.getAllCreatures().size());
        int count = 0;
        for (Creature creature : factory.getAllCreatures()) {
            if (creature.getTemplate() == option.template && creature.getStatus().getModType() == (byte)0 && creature.getStatus().age == age - 1) {
                ++count;
            }
        }
        assertEquals(amount, count);
    }
}
