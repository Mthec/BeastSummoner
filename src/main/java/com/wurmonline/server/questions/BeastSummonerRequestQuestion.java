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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BeastSummonerRequestQuestion extends BeastSummonerQuestionExtension {
    enum State {
        LIST, ADD, DETAILS
    }

    private final Player player;
    private final Creature summoner;
    private final List<SummonRequest.SummonRequestDetails> summons = new ArrayList<>();
    private final List<SummonOption> unusedOptions;
    private final SummonerProfile profile;
    private SummonOption waitingForDetails = null;
    private State state = State.LIST;

    public BeastSummonerRequestQuestion(Player responder, Creature summoner) {
        super(responder, "Beast Summoner", "", CREATURECREATION, summoner.getWurmId());

        this.player = responder;
        this.summoner = summoner;
        List<SummonOption> options = BeastSummonerMod.mod.db.getOptionsFor(summoner);
        if (options == null) {
            unusedOptions = new ArrayList<>();
        } else {
            unusedOptions = new ArrayList<>(options);
            unusedOptions.sort(Comparator.comparing(it -> it.template.getName()));
        }
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
                    sendAddQuestion();
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
                        String property = answers.getProperty("r" + i);
                        if (property != null && property.equals("true")) {
                            SummonRequest.SummonRequestDetails details = summons.remove(i);
                            unusedOptions.add(details.option);
                            unusedOptions.sort(Comparator.comparing(it -> it.template.getName()));
                            break;
                        }
                    }
                    sendQuestion();
                }
                break;
            case ADD:
                if (!wasSelected("back")) {
                    for (Map.Entry<Object, Object> entry : answers.entrySet()) {
                        String val = (String)entry.getKey();
                        if (val != null && val.length() > 0 && entry.getValue().equals("true")) {
                            try {
                                int option = Integer.parseInt(val.substring(1));
                                if (option >= 0 && option < unusedOptions.size()) {
                                    state = State.DETAILS;
                                    waitingForDetails = unusedOptions.get(option);
                                    sendOptionQuestion(waitingForDetails);
                                    return;
                                }
                            } catch (NumberFormatException e) {
                                getResponder().getCommunicator().sendNormalServerMessage("The summoner did not understand what you wanted summoning.");
                            }
                        }
                    }
                }

                state = State.LIST;
                sendQuestion();
                break;
            case DETAILS:
                if (!wasSelected("cancel")) {
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
                    summons.add(new SummonRequest.SummonRequestDetails(waitingForDetails, type, age, amount));
                    unusedOptions.remove(waitingForDetails);
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
                .If(profile.acceptsCoin,
                        b -> b.text("This summoner requires coin as payment."),
                        b -> b.text("This summoner requires " + profile.currency.getName() + " as payment."))
                .newLine()
                .table(new String[] { "Summon", "Amount", "Price", "Remove?" }, summons, (details, b) -> b
                                                                       .label(details.option.template.getName())
                                                                       .label(String.valueOf(details.amount))
                                                                       .label(getPriceString(details.price))
                                                                       .button("r" + i.getAndIncrement(), "x"))
                .button("add", "Add")
                .label("Current total - " + getPriceString(summons.stream().mapToInt(it -> it.price).sum()))
                .newLine()
                .harray(b -> b.button("submit", "Send").spacer().button("cancel", "Cancel"))
                .build();

        getResponder().getCommunicator().sendBml(350, 400, true, true, bml, 200, 200, 200, title);
    }

    private void sendAddQuestion() {
        AtomicInteger i = new AtomicInteger(0);
        String bml = new BMLBuilder(id)
                             .text("Select which beast you would like to summon.")
                             .harray(b -> b.button("back", "Back"))
                             .table(new String[] { "Name", "Price", "Cap", "Type?", "Add" }, unusedOptions, (option, b) ->
                                                        b.label(option.template.getName())
                                                         .label(getPriceString(option.price))
                                                         .label(String.valueOf(option.cap))
                                                         .label(getTypeString(option.allowedTypes))
                                                         .button("a" + i.getAndIncrement(), "Add"))
                             .build();
    }

    private void sendOptionQuestion(SummonOption option) {
        List<Map.Entry<Byte, String>> creatureTypes = CreatureTypeList.creatureTypes.stream().filter(it -> option.allowedTypes.contains(it.getKey())).collect(Collectors.toList());
        String bml = new BMLBuilder(id)
                         .text("Details for - " + option.template.getName())
                         .text("Price per beast - " + option.price)
                         .newLine()
                         .harray(b -> b.label("Amount: ").entry("amount", "1", 3))
                         .harray(b -> b.label("Age (2-100): ").entry("age", "", 3).label("Blank for random."))
                         .label("Creature type:")
                         .If(creatureTypes.isEmpty(), b -> b.radio("type", "0", "No modifier"))
                         .forEach(creatureTypes, (creatureType, b) ->
                                      b.radio("type", Byte.toString(creatureType.getKey()), creatureType.getValue()))
                         .build();

        getResponder().getCommunicator().sendBml(350, 400, true, true, bml, 200, 200, 200, title);
    }

    private String getPriceString(int price) {
        return profile.acceptsCoin ? new Change(price).getChangeShortString() : String.valueOf(price);
    }

    private String getTypeString(Set<Byte> types) {
        int size = types.size();
        if (size == CreatureTypeList.all.size()) {
            return "Yes";
        } else if (size > 1) {
            return "Some";
        } else if (size == 1) {
            return CreatureTypeList.getNameFor(types.iterator().next());
        } else {
            return "None";
        }
    }
}
