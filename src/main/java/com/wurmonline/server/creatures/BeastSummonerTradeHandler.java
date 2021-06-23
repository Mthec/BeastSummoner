package com.wurmonline.server.creatures;

import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.items.*;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;

import java.io.IOException;
import java.util.logging.Logger;

public abstract class BeastSummonerTradeHandler extends TradeHandler {
    protected static final Logger logger = Logger.getLogger(BeastSummonerTradeHandler.class.getName());
    protected final Creature summoner;
    protected final Trade trade;
    protected final SummonerProfile profile;
    protected final String summonName;
    protected final int price;
    protected boolean balanced = false;
    protected boolean waiting = false;

    public BeastSummonerTradeHandler(Creature summoner, Trade trade, SummonerProfile profile, String summonName, int price) {
        super(summoner, trade);
        this.summoner = summoner;
        this.trade = trade;
        this.profile = profile;
        this.summonName = summonName;
        this.price = price;
    }

    @Override
    public void addItemsToTrade() {
        if (trade != null) {
            TradingWindow myOffers = trade.getTradingWindow(1L);
            myOffers.startReceivingItems();

            try {
                Item item = new TempItem(summonName, ItemTemplateFactory.getInstance().getTemplate(ItemList.ratOnAStick), 50f, null);
                if (profile.acceptsCoin) {
                    item.setPrice(price);
                } else {
                    item.setPrice(price * MonetaryConstants.COIN_SILVER);
                }
                myOffers.addItem(item);
            } catch (IOException | NoSuchTemplateException e) {
                trade.creatureOne.getCommunicator().sendAlertServerMessage("Something went wrong in the mists of the void and the summoner forgot what you ordered.");
                e.printStackTrace();
            }

            myOffers.stopReceivingItems();
        }
    }

    @Override
    public int getTraderSellPriceForItem(Item item, TradingWindow window) {
        return item.getPrice();
    }

    @Override
    public int getTraderBuyPriceForItem(Item item) {
        return 0;
    }

    @Override
    void tradeChanged() {
        balanced = false;
        waiting = false;
    }

    @Override
    abstract void balance();
}
