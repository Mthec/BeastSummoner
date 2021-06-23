package com.wurmonline.server.creatures;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.items.TradingWindow;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;

import java.util.logging.Level;

public class BeastSummonerTradeHandlerCurrency extends BeastSummonerTradeHandler {
    public BeastSummonerTradeHandlerCurrency(Creature summoner, Trade trade, SummonerProfile profile, String summonName, int price) {
        super(summoner, trade, profile, summonName, price);
    }

    @Override
    void balance() {
        if (!balanced) {
            if (!waiting) {
                suckCurrency();
                TradingWindow sellWindow = trade.getTradingWindow(3);
                TradingWindow requestWindow = trade.getTradingWindow(4);

                int diff = sellWindow.getAllItems().length - price;
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("diff is " + diff);
                }

                if (diff > 0L) {
                    waiting = true;
                    trade.creatureOne.getCommunicator().sendSafeServerMessage(summoner.getName() + " demands " + diff + " more " + profile.currency.getName() + " to make the trade.");
                    return;
                } else if (diff < 0L) {
                    TradingWindow offerWindow = trade.getTradingWindow(2);
                    while (diff < 0L) {
                        Item first = requestWindow.getItems()[0];
                        requestWindow.removeItem(first);
                        offerWindow.addItem(first);
                        --diff;
                    }
                }

                trade.setSatisfied(summoner, true, trade.getCurrentCounter());
                balanced = true;
            }
        }
    }

    private void suckCurrency() {
        TradingWindow offerWindow = trade.getTradingWindow(2);
        TradingWindow requestWindow = trade.getTradingWindow(4);

        for (Item item : offerWindow.getAllItems()) {
            if (item.getTemplate() == profile.currency) {
                offerWindow.removeItem(item);
                requestWindow.addItem(item);
            }
        }
    }

    private void removeCurrency(TradingWindow sellWindow) {
        for (Item item : sellWindow.getAllItems()) {
            if (item.getTemplate() == profile.currency) {
                sellWindow.removeItem(item);
            }
        }
    }
}
