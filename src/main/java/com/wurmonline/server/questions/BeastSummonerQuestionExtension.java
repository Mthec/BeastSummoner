package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;

@SuppressWarnings("SameParameterValue")
public abstract class BeastSummonerQuestionExtension extends QuestionExtension {
    protected BeastSummonerQuestionExtension(Creature responder, String title, String question, int type, long target) {
        super(responder, title, question, type, target);
    }

    protected String getPrefix() {
        if (BeastSummonerMod.namePrefix.isEmpty()) {
            return "";
        } else {
            return BeastSummonerMod.namePrefix + "_";
        }
    }

    protected String getNameWithoutPrefix(String name) {
        if (BeastSummonerMod.namePrefix.isEmpty() || name.length() < BeastSummonerMod.namePrefix.length() + 1) {
            return name;
        } else {
            return name.substring(BeastSummonerMod.namePrefix.length() + 1);
        }
    }
}
