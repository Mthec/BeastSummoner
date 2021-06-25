package com.wurmonline.server.items;

import com.wurmonline.server.creatures.BeastSummonerTradeHandlerCoins;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.SummonRequest;
import mod.wurmunlimited.npcs.BeastSummonerTest;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class BeastSummonerTradeTests extends BeastSummonerTest {
    private SummonRequest request;
    private BeastSummonerTrade trade;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assert summoner.getShop() != null;
        request = mock(SummonRequest.class);
        trade = new BeastSummonerTrade(player, summoner, request);
        player.setTrade(trade);
        summoner.setTrade(trade);
        BeastSummonerTradeHandlerCoins handler = new BeastSummonerTradeHandlerCoins(summoner, trade);
        ReflectionUtil.setPrivateField(summoner, Creature.class.getDeclaredField("tradeHandler"), handler);
        handler.createTradeItem(profile, "Test", 1);
        handler.addItemsToTrade();
        Item coin = factory.createNewCopperCoin();
        player.getInventory().insertItem(coin);
        trade.getTradingWindow(2).addItem(coin);
        ReflectionUtil.callPrivateMethod(handler, BeastSummonerTradeHandlerCoins.class.getDeclaredMethod("balance"));
    }

    @Test
    void testCorrectTradingWindowsCreated() {
        assertAll(
                () -> assertTrue(trade.getTradingWindow(1) instanceof BeastSummonerTradingWindow),
                () -> assertTrue(trade.getTradingWindow(2) instanceof BeastSummonerTradingWindow),
                () -> assertTrue(trade.getTradingWindow(3) instanceof BeastSummonerTradingWindow),
                () -> assertTrue(trade.getTradingWindow(4) instanceof BeastSummonerTradingWindow)
        );
    }

    @Test
    void testDoSummonCalledOnSuccessfulTrade() {
        trade.setSatisfied(player, true, trade.currentCounter);
        verify(request, times(1)).doSummon();
    }

    @Test
    void testDoSummonNotCalledOnTradeEnded() {
        trade.end(player, true);
        verify(request, never()).doSummon();
    }
}
