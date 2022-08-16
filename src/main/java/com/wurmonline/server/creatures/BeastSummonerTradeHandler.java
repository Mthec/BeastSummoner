package com.wurmonline.server.creatures;

import com.wurmonline.server.Items;
import com.wurmonline.server.economy.BeastSummonerEconomy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.*;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class BeastSummonerTradeHandler extends TradeHandler {
    protected static final Logger logger = Logger.getLogger(BeastSummonerTradeHandler.class.getName());
    protected final Creature summoner;
    protected final Trade trade;
    private final List<Item> tradeItems = new ArrayList<>();
    protected boolean balanced = false;
    protected boolean waiting = false;

    public BeastSummonerTradeHandler(Creature summoner, Trade trade) {
        super(summoner, trade);
        this.summoner = summoner;
        this.trade = trade;
    }

    BeastSummonerTradeHandler(Creature summoner) {
        super();
        this.summoner = summoner;
        this.trade = summoner.getTrade();
    }

    @Override
    public void addItemsToTrade() {
        TradingWindow myOffers = trade.getTradingWindow(3L);
        myOffers.startReceivingItems();

        for (Item item : tradeItems) {
            myOffers.addItem(item);
        }

        myOffers.stopReceivingItems();
    }

    public void createTradeItem(SummonerProfile profile, String summonName, int price) {
        try {
            Item tradeItem = new TempItem(summonName, ItemTemplateFactory.getInstance().getTemplate(ItemList.practiceDoll), 50f, null);
            if (profile.acceptsCoin) {
                tradeItem.setPrice(price);
            } else {
                tradeItem.setPrice(price * MonetaryConstants.COIN_SILVER);
            }
            tradeItem.setOwnerId(summoner.getWurmId());
            tradeItem.setWeight(0, false);
            summoner.getInventory().insertItem(tradeItem);
            tradeItems.add(tradeItem);
        } catch (IOException | NoSuchTemplateException e) {
            trade.creatureOne.getCommunicator().sendAlertServerMessage("Something went wrong in the mists of the void and the summoner forgot what you ordered.");
            e.printStackTrace();
        }
    }

    @Override
    public void end() {
        for (Item tradeItem : tradeItems) {
            Items.destroyItem(tradeItem.getWurmId());
        }
        super.end();
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

    public static TradeHandler create(Creature creature, Trade trade) {
        SummonerProfile profile = BeastSummonerMod.mod.db.getProfileFor(creature);

        if (profile == null) {
            logger.warning("BeastSummonerTradeHandler.create was called but no profile was found.");
            return null;
        }

        if (profile.acceptsCoin) {
            return new BeastSummonerTradeHandlerCoins(creature, trade);
        } else {
            return new BeastSummonerTradeHandlerCurrency(creature, trade);
        }
    }

    public static BeastSummonerTradeHandler bypassConstructor(Creature creature) {
        Trade trade = creature.getTrade();
        SummonerProfile profile = BeastSummonerMod.mod.db.getProfileFor(creature);

        if (profile == null) {
            logger.warning("BeastSummonerTradeHandler.create was called but no profile was found.");
            return null;
        }

        BeastSummonerTradeHandler handler;
        if (profile.acceptsCoin) {
            handler = new BeastSummonerTradeHandlerCoins(creature);
        } else {
            handler = new BeastSummonerTradeHandlerCurrency(creature);
        }

        try {
            ReflectionUtil.setPrivateField(handler, TradeHandler.class.getDeclaredField("creature"), creature);
            ReflectionUtil.setPrivateField(handler, TradeHandler.class.getDeclaredField("trade"), trade);
            Shop shop = BeastSummonerEconomy.findOrCreateShopFor(creature.getWurmId());
            boolean ownerTrade = shop.getOwnerId() == handler.trade.creatureOne.getWurmId();
            handler.setOwnerTrade(ownerTrade);
            if (ownerTrade || handler.trade.creatureOne.getPower() >= 3) {
                handler.trade.creatureOne.getCommunicator().sendSafeServerMessage(creature.getName() + " says, 'Welcome back, " + handler.trade.creatureOne.getName() + "!  Please pay for your request.'");
            } else {
                handler.trade.creatureOne.getCommunicator().sendSafeServerMessage(creature.getName() + " says, 'Please pay for your request.'");
            }

            ReflectionUtil.setPrivateField(creature, Creature.class.getDeclaredField("tradeHandler"), handler);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.warning("Failed to set private field:");
            e.printStackTrace();
        }

        return handler;
    }

    private void setOwnerTrade(boolean isOwnerTrade) throws NoSuchFieldException, IllegalAccessException {
        ReflectionUtil.setPrivateField(this, TradeHandler.class.getDeclaredField("ownerTrade"), isOwnerTrade);
    }
}
