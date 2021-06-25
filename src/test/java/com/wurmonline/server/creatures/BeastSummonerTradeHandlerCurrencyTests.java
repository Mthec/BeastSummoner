package com.wurmonline.server.creatures;

import com.wurmonline.server.items.*;
import com.wurmonline.server.questions.SummonRequest;
import mod.wurmunlimited.npcs.BeastSummonerTest;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class BeastSummonerTradeHandlerCurrencyTests extends BeastSummonerTest {
    private int price;
    private int currency;
    private Trade trade;
    private BeastSummonerTradeHandler handler;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        currency = ItemList.medallionHota;
        summoner = factory.createNewBeastSummoner(currency);
        profile = BeastSummonerMod.mod.db.getProfileFor(summoner);
        assert summoner.getShop() != null;
        SummonRequest example = exampleSummon();
        trade = new BeastSummonerTrade(player, summoner, example);
        player.setTrade(trade);
        summoner.setTrade(trade);
        price = example.totalPrice();
        assert price == 2;
        handler = new BeastSummonerTradeHandlerCurrency(summoner, trade);
        ReflectionUtil.setPrivateField(summoner, Creature.class.getDeclaredField("tradeHandler"), handler);
        handler.createTradeItem(profile, "Test", price);
        handler.addItemsToTrade();
    }

    // Balance

    private void addCurrencyToTrade(int amount) {
        for (int i = 0; i < amount; i++) {
            Item item = factory.createNewItem(currency);
            trade.getTradingWindow(4).addItem(item);
        }
    }

    @Test
    void testBalancesOnce() throws NoSuchFieldException, IllegalAccessException {
        addCurrencyToTrade(2);

        assertFalse((boolean)ReflectionUtil.getPrivateField(trade, BaseTrade.class.getDeclaredField("creatureTwoSatisfied")));
        handler.balance();
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, BaseTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testOnlyCurrencySucked() {
        Item item = factory.createNewItem(currency);
        Item coin = factory.createNewCopperCoin();
        trade.getTradingWindow(2).addItem(item);
        trade.getTradingWindow(2).addItem(coin);

        handler.balance();
        assertEquals(1, trade.getTradingWindow(2).getAllItems().length);
        assertEquals(1, trade.getTradingWindow(4).getAllItems().length);
        assertEquals(currency, trade.getTradingWindow(4).getAllItems()[0].getTemplateId());
    }

    @Test
    void testPrice() {
        addCurrencyToTrade(price);

        handler.balance();
        assertThat(player, didNotReceiveMessageContaining("demands"));
        assertEquals(price, trade.getTradingWindow(4).getAllItems().length);
    }

    @Test
    void testPositiveDiffSendsMessage() throws NoSuchFieldException, IllegalAccessException {
        addCurrencyToTrade(1);

        handler.balance();
        assertThat(player, receivedMessageContaining("demands 1 more "));
        assertFalse((boolean)ReflectionUtil.getPrivateField(trade, BaseTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testNegativeDiffRemovesCurrency() throws NoSuchFieldException, IllegalAccessException {
        addCurrencyToTrade(3);

        handler.balance();
        assertThat(player, didNotReceiveMessageContaining("demands"));
        assertEquals(1, trade.getTradingWindow(2).getAllItems().length);
        assertEquals(2, trade.getTradingWindow(4).getAllItems().length);
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, BaseTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void test0DiffDoesNotSendMessageOrAddMoney() throws NoSuchFieldException, IllegalAccessException {
        addCurrencyToTrade(2);

        handler.balance();
        assertThat(player, didNotReceiveMessageContaining("demands"));
        assertEquals(1, trade.getTradingWindow(3).getAllItems().length);
        assertEquals(2, trade.getTradingWindow(4).getAllItems().length);
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, BaseTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testConsecutiveBalancesDoNotAlterSelectedCurrency() {
        addCurrencyToTrade(3);

        assertEquals(3, trade.getTradingWindow(4).getAllItems().length);
        handler.balance();
        assertEquals(2, trade.getTradingWindow(4).getAllItems().length);
        handler.tradeChanged();
        handler.balance();
        assertEquals(2, trade.getTradingWindow(4).getAllItems().length);
    }
}
