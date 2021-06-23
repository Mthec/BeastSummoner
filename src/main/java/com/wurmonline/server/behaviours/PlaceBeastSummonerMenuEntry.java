package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.PlaceBeastSummonerQuestion;
import com.wurmonline.server.zones.VolaTile;

public class PlaceBeastSummonerMenuEntry implements NpcMenuEntry {
    public PlaceBeastSummonerMenuEntry() {
        PlaceNpcMenu.addNpcAction(this);
    }

    @Override
    public String getName() {
        return "Beast Summoner";
    }

    @Override
    public boolean doAction(Action action, short num, Creature performer, Item source, VolaTile tile, int floorLevel) {
        new PlaceBeastSummonerQuestion(performer, tile, floorLevel);
        return true;
    }
}
