package com.wurmonline.server.items;

import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Economy;

public class BeastSummonerTradingWindow<T> extends BaseTradingWindow<BeastSummonerTrade> {
    protected BeastSummonerTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId, BeastSummonerTrade trade) {
        super(owner, watcher, offer, wurmId, trade);
    }

    @Override
    protected String getLoggerNamePrefix() {
        return "beast_summoner_";
    }

    @Override
    protected int handleItemTransfer(Item item) {
        if (wurmId == 4) {
            if (item.isCoin()) {
                Economy.getEconomy().returnCoin(item, "Beast Summoner");
                return Economy.getValueFor(item.getTemplateId());
            } else {
                Items.destroyItem(item.getWurmId());
                return 1;
            }
        } else if (wurmId == 3) {
            if (item.isCoin()) {
                watcher.getInventory().insertItem(item);
            }
        }

        return 0;
    }
}
