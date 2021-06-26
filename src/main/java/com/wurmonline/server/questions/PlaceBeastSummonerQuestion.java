package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.zones.VolaTile;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTemplate;

import java.sql.SQLException;
import java.util.Properties;

public class PlaceBeastSummonerQuestion extends BeastSummonerPlaceOrManageQuestion {
    private final VolaTile tile;
    private final int floorLevel;

    public PlaceBeastSummonerQuestion(Creature responder, VolaTile tile, int floorLevel) {
        super(responder);
        this.tile = tile;
        this.floorLevel = floorLevel;
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);
        Creature responder = getResponder();

        if (doFilter()) {
            sendQuestion();
        } else {
            byte sex = getGender();
            String name = getName(sex);
            String tag = getTag();

            if (locationIsValid(responder, tile)) {
                try {
                    Creature summoner = BeastSummonerTemplate.createNewSummoner(tile, floorLevel, name, sex, responder.getKingdomId(), getCurrencyTemplate(), tag);
                    logger.info(responder.getName() + " created a summoner: " + summoner.getWurmId());
                    checkSaveModel(summoner);
                } catch (SQLException e) {
                    responder.getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The summoner was created, but some of their details were not set.");
                    e.printStackTrace();
                } catch (Exception e) {
                    responder.getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The summoner was not created.");
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void sendQuestion() {
        BML bml = new BMLBuilder(id)
                          .text("Place Beast Summoner").bold()
                          .text("Place an NPC that will offer creature summoning services.")
                          .text("This trader will only accept a certain type of item in exchange for goods.");

        getResponder().getCommunicator().sendBml(450, 400, true, true, endBML(middleBML(bml, "")), 200, 200, 200, title);
    }
}
