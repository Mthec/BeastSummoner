package com.wurmonline.server.economy;

import java.util.logging.Logger;

public class BeastSummonerEconomy {
    private static final Logger logger = Logger.getLogger(BeastSummonerEconomy.class.getName());

    public static Shop createShop(long wurmId) {
        return new DbShop(wurmId, 0);
    }

    public static Shop findOrCreateShopFor(long wurmId) {
        for (Shop shop : Economy.getEconomy().getShops()) {
            if (shop.ownerId == wurmId) {
                logger.warning("Shop for summoner (" + wurmId + ") did exist, please report.");
                return shop;
            }
        }

        logger.warning("Shop for summoner (" + wurmId + ") did not exist, creating a new one.");
        return createShop(wurmId);
    }
}
