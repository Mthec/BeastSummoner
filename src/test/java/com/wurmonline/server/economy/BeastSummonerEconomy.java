package com.wurmonline.server.economy;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerObjectsFactory;

import java.util.logging.Logger;

public class BeastSummonerEconomy {
    private static final Logger logger = Logger.getLogger(BeastSummonerEconomy.class.getName());

    public static Shop createShop(long wurmId) {
        BeastSummonerObjectsFactory factory = ((BeastSummonerObjectsFactory)BeastSummonerObjectsFactory.getCurrent());
        try {
            Creature trader = factory.getCreature(wurmId);
            FakeShop shop = FakeShop.createFakeTraderShop(wurmId);
            factory.addShop(trader, shop);
            return shop;
        } catch (NoSuchCreatureException e) {
            throw new RuntimeException(e);
        }
    }

    public static Shop findOrCreateShopFor(long wurmId) {
        try {
            Shop shop = BeastSummonerObjectsFactory.getCurrent().getShop(BeastSummonerObjectsFactory.getCurrent().getCreature(wurmId));
            if (shop == null) {
                throw new RuntimeException("Did not find shop for creature - " + wurmId);
            } else {
                return shop;
            }
        } catch (NoSuchCreatureException e) {
            throw new RuntimeException(e);
        }
    }
}

