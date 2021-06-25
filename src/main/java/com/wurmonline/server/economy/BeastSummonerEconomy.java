package com.wurmonline.server.economy;


public class BeastSummonerEconomy {
    public static void createShop(long wurmId) {
        new DbShop(wurmId, 0);
    }
}
