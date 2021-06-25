package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.BeastSummonerTradeHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.items.BeastSummonerTrade;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;
import mod.wurmunlimited.npcs.beastsummoner.SummonOption;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class BeastSummonerRequestQuestion extends BeastSummonerQuestionExtension {
    enum State {
        LIST, ADD
    }
    private final Player player;
    private final Creature summoner;
    private final List<SummonRequest.SummonRequestDetails> summons = new ArrayList<>();
    private final List<SummonOption> options;
    private final SummonerProfile profile;
    private State state = State.LIST;

    public BeastSummonerRequestQuestion(Player responder, Creature summoner) {
        super(responder, "Beast Summoner", "", CREATURECREATION, summoner.getWurmId());
        this.player = responder;
        this.summoner = summoner;
        options = BeastSummonerMod.mod.db.getOptionsFor(summoner);
        profile = BeastSummonerMod.mod.db.getProfileFor(summoner);
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);

        if (wasSelected("cancel") && state == State.LIST)
            return;

        Creature responder = getResponder();
        switch (state) {
            case LIST:
                if (wasSelected("add")) {
                    state = State.ADD;
                    sendAddOption();
                } else if (wasSelected("submit")) {
                    SummonRequest request = new SummonRequest(summoner, player, profile, summons);
                    BeastSummonerTrade trade = new BeastSummonerTrade(responder, summoner, request);

                    responder.setTrade(trade);
                    summoner.setTrade(trade);
                    summoner.getCommunicator().sendStartTrading(responder);
                    responder.getCommunicator().sendStartTrading(summoner);
                    //noinspection ConstantConditions
                    BeastSummonerTradeHandler handler = (BeastSummonerTradeHandler)summoner.getTradeHandler();
                    for (SummonRequest.SummonRequestDetails details : summons) {
                        handler.createTradeItem(profile, details.name, details.price);
                    }
                } else {
                    for (int i = 0; i < summons.size(); i++) {
                        String property = answers.getProperty("remove" + i);
                        if (property != null && property.equals("true")) {
                            summons.remove(i);
                            break;
                        }
                    }
                    sendQuestion();
                }
                break;
            case ADD:
                if (!wasSelected("cancel")) {
                    for (Map.Entry<Object, Object> toAdd : answers.entrySet()) {
                        String key = (String)toAdd.getKey();
                        if (key.startsWith("add") && toAdd.getValue().equals("true")) {
                            int id;
                            try {
                                id = Integer.parseInt(key.substring(3));
                            } catch (NumberFormatException e) {
                                responder.getCommunicator().sendAlertServerMessage("The summoner looks confused and forgets what they are doing.");
                                logger.warning("Invalid template selection - " + toAdd.getKey() + "/" + toAdd.getValue() + ".");
                                break;
                            }

                            SummonOption option = options.get(id);
                            byte age;
                            try {
                                age = Byte.parseByte(answers.getProperty("age"));
                            } catch (NumberFormatException e) {
                                responder.getCommunicator().sendNormalServerMessage("The summoner didn't understand the age, so will select randomly.");
                                age = 0;
                            }
                            byte type;
                            try {
                                type = Byte.parseByte(answers.getProperty("type"));
                            } catch (NumberFormatException e) {
                                responder.getCommunicator().sendNormalServerMessage("The summoner didn't understand the creature type, so will select normal.");
                                type = 0;
                            }
                            int amount;
                            try {
                                amount = Integer.parseInt(answers.getProperty("amount"));
                            } catch (NumberFormatException e) {
                                responder.getCommunicator().sendNormalServerMessage("The summoner didn't understand how many you wanted, so will provide 1.");
                                amount = 1;
                            }
                            summons.add(new SummonRequest.SummonRequestDetails(option, type, age, amount));
                            break;
                        }
                    }
                }
                state = State.LIST;
                sendQuestion();
                break;
        }
    }

    @Override
    public void sendQuestion() {
        AtomicInteger i = new AtomicInteger(0);
        String bml = new BMLBuilder(id)
                .text("Create the list of beasts you would like to summon.")
                .newLine()
                .table(new String[] { "Summon", "Amount", "Price", "Remove?" }, summons, (details, b) -> b
                                                                       .label(details.option.template.getName())
                                                                       .label(String.valueOf(details.amount))
                                                                       .label(getPriceString(details.price))
                                                                       .button("remove" + i.getAndIncrement(), "Remove"))
                .button("add", "Add")
                .newLine()
                .harray(b -> b.button("submit", "Send").spacer().button("cancel", "Cancel"))
                .build();

        getResponder().getCommunicator().sendBml(350, 400, true, true, bml, 200, 200, 200, title);
    }

    private void sendAddOption() {
        AtomicInteger i = new AtomicInteger(0);
        String bml = new BMLBuilder(id)
                         .text("Select which beast you would like to summon.")
                         .newLine()
                         .harray(b -> b.label("Age (2-100): ").entry("age", "", 3).label("Blank for random."))
                         .table(new String[] { "Name", "Price", "Add" }, options, (option, b) ->
                                         b.label(option.template.getName())
                                          .label(getPriceString(option.price))
                                          .button("add" + i.getAndIncrement(), "Add"))
                         .build();

        getResponder().getCommunicator().sendBml(350, 400, true, true, bml, 200, 200, 200, title);
    }

    private String getPriceString(int price) {
        return profile.acceptsCoin ? new Change(price).getChangeShortString() : String.valueOf(price);
    }
}
