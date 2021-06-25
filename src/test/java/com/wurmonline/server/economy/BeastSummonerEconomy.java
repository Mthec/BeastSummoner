package com.wurmonline.server.economy;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import mod.wurmunlimited.npcs.BeastSummonerObjectsFactory;

public class BeastSummonerEconomy {
    public static void createShop(long wurmId) {
        BeastSummonerObjectsFactory factory = ((BeastSummonerObjectsFactory)BeastSummonerObjectsFactory.getCurrent());
        try {
            Creature trader = factory.getCreature(wurmId);
            factory.addShop(trader, FakeShop.createFakeTraderShop(wurmId));
        } catch (NoSuchCreatureException e) {
            throw new RuntimeException(e);
        }
    }
}

