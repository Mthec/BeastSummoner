package com.wurmonline.server.creatures;

import com.wurmonline.server.items.*;
import com.wurmonline.server.questions.SummonRequest;
import mod.wurmunlimited.npcs.BeastSummonerTest;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class BeastSummonerTradeHandlerCoinTests extends BeastSummonerTest {
    private int price;
    private Trade trade;
    private BeastSummonerTradeHandler handler;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        assert summoner.getShop() != null;
        SummonRequest example = exampleSummon();
        trade = new BeastSummonerTrade(player, summoner, example);
        player.setTrade(trade);
        summoner.setTrade(trade);
        price = example.totalPrice();
        assert price == 2;
        handler = new BeastSummonerTradeHandlerCoins(summoner, trade);
        ReflectionUtil.setPrivateField(summoner, Creature.class.getDeclaredField("tradeHandler"), handler);
        handler.createTradeItem(profile, "Test", price);
        handler.addItemsToTrade();
    }

    // Balance

    private void addCoinToTrade(int amount) {
        for (int i = 0; i < amount; i++) {
            Item item = factory.createNewItem(ItemList.coinIron);
            trade.getTradingWindow(4).addItem(item);
        }
    }

    @Test
    void testBalancesOnce() throws NoSuchFieldException, IllegalAccessException {
        addCoinToTrade(2);

        assertFalse((boolean)ReflectionUtil.getPrivateField(trade, BaseTrade.class.getDeclaredField("creatureTwoSatisfied")));
        handler.balance();
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, BaseTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testOnlyCoinSucked() {
        int currency = ItemList.almanac;
        Item item = factory.createNewItem(currency);
        Item coin = factory.createNewCopperCoin();
        trade.getTradingWindow(2).addItem(item);
        trade.getTradingWindow(2).addItem(coin);

        handler.balance();
        assertEquals(1, trade.getTradingWindow(2).getAllItems().length);
        assertEquals(1, trade.getTradingWindow(4).getAllItems().length);
        assertEquals(currency, trade.getTradingWindow(2).getAllItems()[0].getTemplateId());
    }

    @Test
    void testPrice() {
        addCoinToTrade(price);

        handler.balance();
        assertThat(player, didNotReceiveMessageContaining("demands"));
        assertEquals(price, trade.getTradingWindow(4).getAllItems().length);
    }

    @Test
    void testPositiveDiffSendsMessage() throws NoSuchFieldException, IllegalAccessException {
        addCoinToTrade(1);

        handler.balance();
        assertThat(player, receivedMessageContaining("demands 1 iron coin"));
        assertFalse((boolean)ReflectionUtil.getPrivateField(trade, BaseTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testNegativeDiffAddsCoin() throws NoSuchFieldException, IllegalAccessException {
        addCoinToTrade(3);

        handler.balance();
        assertThat(player, didNotReceiveMessageContaining("demands"));
        assertEquals(0, trade.getTradingWindow(2).getAllItems().length);
        assertEquals(3, trade.getTradingWindow(4).getAllItems().length);
        assertThat(Arrays.asList(trade.getTradingWindow(3).getAllItems()), containsCoinsOfValue(1));
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, BaseTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void test0DiffDoesNotSendMessageOrAddMoney() throws NoSuchFieldException, IllegalAccessException {
        addCoinToTrade(2);

        handler.balance();
        assertThat(player, didNotReceiveMessageContaining("demands"));
        assertEquals(1, trade.getTradingWindow(3).getAllItems().length);
        assertEquals(2, trade.getTradingWindow(4).getAllItems().length);
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, BaseTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testConsecutiveBalancesDoNotAlterSelectedCoins() {
        addCoinToTrade(3);

        assertEquals(3, trade.getTradingWindow(4).getAllItems().length);
        handler.balance();
        assertEquals(3, trade.getTradingWindow(4).getAllItems().length);
        handler.tradeChanged();
        handler.balance();
        assertEquals(3, trade.getTradingWindow(4).getAllItems().length);
    }

    @Test
    void testCoinMessage() throws NoSuchFieldException, IllegalAccessException {
        handler.createTradeItem(profile, "Test 2", 1234567);
        handler.addItemsToTrade();
        addCoinToTrade(2);

        handler.balance();
        assertThat(player, receivedMessageContaining("demands 1 gold, 23 silver, 45 copper and 67 iron"));
        assertFalse((boolean)ReflectionUtil.getPrivateField(trade, BaseTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }
}
