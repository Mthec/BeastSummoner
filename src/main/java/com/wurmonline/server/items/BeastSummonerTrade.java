package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.SummonRequest;

import java.util.function.Supplier;

public class BeastSummonerTrade extends BaseTrade<Void> {
    private final SummonRequest summons;

    public BeastSummonerTrade(Creature player, Creature summoner, SummonRequest summons) {
        super(player, summoner, () -> null);
        this.summons = summons;
    }

    @Override
    protected TradingWindow createTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId, Supplier<Void> value) {
        return new BeastSummonerTradingWindow<>(owner, watcher, offer, wurmId, this);
    }

    @Override
    protected void onSuccessfulTrade(Creature creature) {
        summons.doSummon();
    }
}
