package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
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

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);

        if (doFilter()) {
            sendQuestion();
        } else if (wasSelected("confirm")) {
            checkSaveName(summoner);
            checkSaveModel(summoner);
            checkSaveCurrency(summoner);
            checkSaveTag(summoner, currentTag);
        } else if (wasSelected("edit")) {
            new BeastSummonerEditTagsQuestion(getResponder()).sendQuestion();
        } else if (wasSelected("list")) {
            new BeastSummonerSummonsListQuestion(getResponder(), summoner).sendQuestion();
        } else if (wasSelected("dismiss")) {
            tryDismiss(summoner);
        }
    }

    @Override
    public void sendQuestion() {
        Shop shop = Economy.getEconomy().getShop(summoner);
        if (shop == null) {
            logger.warning("Summoner shop was null, please report.");
            return;
        }

        BML bml = middleBML(new BMLBuilder(id), getNameWithoutPrefix(summoner.getName()))
                          .text("Sales:")
                          .table(new String[] { "This month", "Total" }, Collections.singletonList(1),
                                  (v, b) -> b.label(String.valueOf(shop.getMoneyEarnedMonth())).label(String.valueOf(shop.getMoneyEarnedLife())))
                          .newLine();

        getResponder().getCommunicator().sendBml(400, 350, true, true, endBML(bml, currentTag, summoner), 200, 200, 200, title);
    }
}
