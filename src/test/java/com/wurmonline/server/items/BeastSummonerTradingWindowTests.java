package com.wurmonline.server.items;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.creatures.BeastSummonerTradeHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Shop;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class BeastSummonerTradingWindowTests extends BeastSummonerTest {
    private BeastSummonerTrade trade;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        trade = new BeastSummonerTrade(player, summoner, exampleSummon());
        BeastSummonerTradeHandler handler = mock(BeastSummonerTradeHandler.class);
        ReflectionUtil.setPrivateField(summoner, Creature.class.getDeclaredField("tradeHandler"), handler);
    }

    @Test
    void testHandleItemTransferCoin() {
        BeastSummonerTradingWindow<BeastSummonerTrade> tradingWindow = new BeastSummonerTradingWindow<>(player, summoner, false, 4, trade);
        Item coin = factory.createNewCopperCoin();
        tradingWindow.addItem(coin);
        assert !coin.isBanked();
        assertEquals(100, tradingWindow.handleItemTransfer(coin));
        assertTrue(coin.isBanked());
    }

    @Test
    void testHandleItemTransferCurrency() throws NoSuchFieldException, IllegalAccessException {
        BeastSummonerTradingWindow<BeastSummonerTrade> tradingWindow = new BeastSummonerTradingWindow<>(player, summoner, false, 4, trade);
        Item currency = factory.createNewItem();
        assert !currency.isCoin();
        tradingWindow.addItem(currency);
        assertEquals(1, tradingWindow.handleItemTransfer(currency));
        assertThrows(NoSuchItemException.class, () -> Items.getItem(currency.getWurmId()));
    }
    @Test
    void testHandleItemTransferOtherWindows() {
        BeastSummonerTradingWindow<BeastSummonerTrade> tradingWindow1 = new BeastSummonerTradingWindow<>(summoner, player, true, 1, trade);
        BeastSummonerTradingWindow<BeastSummonerTrade> tradingWindow2 = new BeastSummonerTradingWindow<>(player, summoner, true, 2, trade);
        BeastSummonerTradingWindow<BeastSummonerTrade> tradingWindow3 = new BeastSummonerTradingWindow<>(summoner, player, false, 3, trade);

        for (BeastSummonerTradingWindow<BeastSummonerTrade> tradingWindow : Arrays.asList(tradingWindow1, tradingWindow2, tradingWindow3)) {
            Item coin = factory.createNewCopperCoin();
            tradingWindow.addItem(coin);
            assert !coin.isBanked();
            assertEquals(0, tradingWindow.handleItemTransfer(coin));
            assertFalse(coin.isBanked());
        }
    }

    @Test
    void testShopUpdatedCorrectly() {
        BeastSummonerTradingWindow<BeastSummonerTrade> tradingWindow = new BeastSummonerTradingWindow<>(player, summoner, false, 4, trade);
        Item coin = factory.createNewCopperCoin();
        tradingWindow.addItem(coin);
        tradingWindow.swapOwners();
        Shop shop = factory.getShop(summoner);
        assert shop != null;
        assertEquals(100, shop.getMoneyEarnedMonth());
        assertEquals(100, shop.getMoneyEarnedLife());
    }
}
