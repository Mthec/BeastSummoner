package com.wurmonline.server.creatures;

import com.wurmonline.server.items.BeastSummonerTrade;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.items.Trade;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeastSummonerTradeHandlerTests extends BeastSummonerTest {
    @Test
    void testCoinsHandlerReturned() {
        assert profile.acceptsCoin;
        Trade trade = new BeastSummonerTrade(player, summoner, exampleSummon());
        assertTrue(BeastSummonerTradeHandler.create(summoner, trade) instanceof BeastSummonerTradeHandlerCoins);
    }

    @Test
    void testCurrencyHandlerReturned() throws SQLException, NoSuchTemplateException {
        BeastSummonerMod.mod.db.setCurrencyFor(summoner, ItemList.acorn);
        profile = BeastSummonerMod.mod.db.getProfileFor(summoner);
        assert !Objects.requireNonNull(profile).acceptsCoin;
        Trade trade = new BeastSummonerTrade(player, summoner, exampleSummon());
        assertTrue(BeastSummonerTradeHandler.create(summoner, trade) instanceof BeastSummonerTradeHandlerCurrency);
    }
}
