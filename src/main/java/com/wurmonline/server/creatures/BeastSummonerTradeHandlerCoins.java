package com.wurmonline.server.creatures;

import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.items.TradingWindow;

import java.util.Arrays;
import java.util.logging.Level;

public class BeastSummonerTradeHandlerCoins extends BeastSummonerTradeHandler {
    public BeastSummonerTradeHandlerCoins(Creature summoner, Trade trade) {
        super(summoner, trade);
    }

    @Override
    void balance() {
        if (!balanced) {
            if (!waiting) {
                suckCoins();
                TradingWindow sellWindow = trade.getTradingWindow(3);
                TradingWindow requestWindow = trade.getTradingWindow(4);
                removeCoins(sellWindow);

                int diff = Arrays.stream(sellWindow.getAllItems()).mapToInt(i -> getTraderSellPriceForItem(i, sellWindow)).sum() -
                                   Arrays.stream(requestWindow.getAllItems()).mapToInt(Item::getValue).sum();
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("diff is " + diff);
                }

                if (diff > 0L) {
                    waiting = true;
                    Change change = new Change(diff);
                    trade.creatureOne.getCommunicator().sendSafeServerMessage(summoner.getName() + " demands " + change.getChangeString() + " coins to make the trade.");
                } else if (diff < 0L) {
                    Item[] money = Economy.getEconomy().getCoinsFor(Math.abs(diff));
                    sellWindow.startReceivingItems();

                    for (Item item : money) {
                        sellWindow.addItem(item);
                    }

                    sellWindow.stopReceivingItems();
                    trade.setSatisfied(summoner, true, this.trade.getCurrentCounter());
                    balanced = true;
                } else {
                    trade.setSatisfied(summoner, true, trade.getCurrentCounter());
                    balanced = true;
                }
            }
        }
    }

    private void suckCoins() {
        TradingWindow offerWindow = trade.getTradingWindow(2);
        TradingWindow requestWindow = trade.getTradingWindow(4);

        for (Item item : offerWindow.getAllItems()) {
            if (item.isCoin()) {
                offerWindow.removeItem(item);
                requestWindow.addItem(item);
            }
        }
    }

    private void removeCoins(TradingWindow sellWindow) {
        for (Item item : sellWindow.getAllItems()) {
            if (item.isCoin()) {
                sellWindow.removeItem(item);
            }
        }
    }
}
