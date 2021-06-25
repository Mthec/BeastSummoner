package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplateFactory;
import com.wurmonline.server.creatures.CreatureTemplateIds;
import com.wurmonline.server.creatures.NoSuchCreatureTemplateException;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.beastsummoner.SummonOption;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;

import java.util.Collections;

public class ExampleSummonerRequest {
    public static SummonRequest exampleSummon(Creature summoner, Player player, SummonerProfile profile) {
        try {
            return new SummonRequest(summoner, player, profile,
                    Collections.singletonList(new SummonRequest.SummonRequestDetails(
                            new SummonOption(CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.ANACONDA_CID), 1, 10),
                            (byte)0, (byte)0, 2)));
        } catch (NoSuchCreatureTemplateException e) {
            throw new RuntimeException(e);
        }
    }
}
