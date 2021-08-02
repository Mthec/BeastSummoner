package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.BeastSummonerEconomy;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;

import java.util.Collections;
import java.util.Properties;

public class BeastSummonerManagementQuestion extends BeastSummonerPlaceOrManageQuestion {
    private final Creature summoner;
    private final String currentTag;

    public BeastSummonerManagementQuestion(Creature responder, Creature summoner) {
        super(responder, summoner);
        this.summoner = summoner;
        currentTag = BeastSummonerMod.mod.db.getTagFor(summoner);
    }

    private BeastSummonerManagementQuestion(Creature responder, Creature summoner, String currentTag) {
        super(responder, summoner);
        this.summoner = summoner;
        this.currentTag = currentTag;
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);

        if (doFilter()) {
            new BeastSummonerManagementQuestion(getResponder(), summoner, currentTag).sendQuestion();
        } else if (wasSelected("confirm")) {
            checkSaveName(summoner);
            checkSaveCurrency(summoner);
            checkSaveTag(summoner, currentTag);
        } else if (wasSelected("edit")) {
            new BeastSummonerEditTagsQuestion(getResponder()).sendQuestion();
        } else if (wasSelected("list")) {
            new BeastSummonerSummonsListQuestion(getResponder(), summoner).sendQuestion();
        } else if (wasSelected("customise")) {
            new CreatureCustomiserQuestion(getResponder(), summoner, BeastSummonerMod.mod.faceSetter, BeastSummonerMod.mod.modelSetter, modelOptions).sendQuestion();
        } else if (wasSelected("dismiss")) {
            tryDismiss(summoner);
        }
    }

    @Override
    public void sendQuestion() {
        Shop shop = Economy.getEconomy().getShop(summoner);
        if (shop == null) {
            logger.warning("Summoner shop was null.");
            shop = BeastSummonerEconomy.findOrCreateShopFor(summoner.getWurmId());
        }
        final Shop finalShop = shop;

        BML bml = new BMLBuilder(id).text("Sales:")
                          .table(new String[] { "This month", "Total" }, Collections.singletonList(1),
                                  (v, b) -> b.label(String.valueOf(finalShop.getMoneyEarnedMonth())).label(String.valueOf(finalShop.getMoneyEarnedLife())))
                          .newLine();

        getResponder().getCommunicator().sendBml(425, 350, true, true, endBML(middleBML(bml, getNameWithoutPrefix(summoner.getName())), currentTag, summoner), 200, 200, 200, title);
    }
}
