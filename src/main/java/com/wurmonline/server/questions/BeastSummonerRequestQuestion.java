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
    private final List<SummonRequest.SummonRequestDetails> summons;
    private final List<SummonOption> unusedOptions;
    private final SummonerProfile profile;
    private SummonOption waitingForDetails = null;
    private final State state;

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
        summons = new ArrayList<>();
        state = State.LIST;
    }

    private BeastSummonerRequestQuestion(BeastSummonerRequestQuestion oldQuestion, State newState) {
        super(oldQuestion.getResponder(), "Beast Summoner", "", CREATURECREATION, oldQuestion.summoner.getWurmId());
        player = oldQuestion.player;
        summoner = oldQuestion.summoner;
        summons = oldQuestion.summons;
        unusedOptions = oldQuestion.unusedOptions;
        profile = oldQuestion.profile;
        state = newState;

        switch (state) {
            case LIST:
                sendQuestion();
                break;
            case ADD:
                sendAddQuestion();
                break;
            case DETAILS:
                waitingForDetails = oldQuestion.waitingForDetails;
                sendOptionQuestion(waitingForDetails);
                break;
        }
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
                    new BeastSummonerRequestQuestion(this, State.ADD);
                } else if (wasSelected("submit")) {
                    if (summons.size() == 0) {
                        responder.getCommunicator().sendNormalServerMessage("You decide not to summon anything at this time.");
                        return;
                    }
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
                    handler.addItemsToTrade();
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
                    new BeastSummonerRequestQuestion(this, State.LIST);
                }
                break;
            case ADD:
                if (!wasSelected("back")) {
                    for (Map.Entry<Object, Object> entry : answers.entrySet()) {
                        String val = (String)entry.getKey();
                        if (val != null && val.startsWith("a") && entry.getValue().equals("true")) {
                            try {
                                int option = Integer.parseInt(val.substring(1));
                                if (option >= 0 && option < unusedOptions.size()) {
                                    waitingForDetails = unusedOptions.get(option);
                                    new BeastSummonerRequestQuestion(this, State.DETAILS);
                                    return;
                                }
                                throw new NumberFormatException();
                            } catch (NumberFormatException e) {
                                responder.getCommunicator().sendSafeServerMessage(summoner.getName() + " says 'I do not understand what you want me to summon.'");
                            }
                        }
                    }
                }

                new BeastSummonerRequestQuestion(this, State.LIST);
                break;
            case DETAILS:
                if (!wasSelected("cancel")) {
                    int amount;
                    try {
                        amount = Integer.parseInt(answers.getProperty("amount"));
                    } catch (NumberFormatException e) {
                        responder.getCommunicator().sendNormalServerMessage(summoner.getName() + " didn't understand how many " + waitingForDetails.template.getName() + " you wanted, so will provide 1.");
                        amount = 1;
                    }
                    if (amount > waitingForDetails.cap) {
                        amount = waitingForDetails.cap;
                        responder.getCommunicator().sendSafeServerMessage(summoner.getName() + " says 'You cannot summon that many " + waitingForDetails.template.getName() + ".'");
                    } else if (amount < 1) {
                        amount = 1;
                        responder.getCommunicator().sendNormalServerMessage("Amount must be 1 or greater, setting 1.");
                    }
                    byte age;
                    try {
                        age = Byte.parseByte(answers.getProperty("age"));
                        if (age < 0) {
                            age = 0;
                            responder.getCommunicator().sendNormalServerMessage("Age must be 0 or greater than 2, so it will be selected randomly.");
                        } else if (age == 1) {
                            age = 2;
                            responder.getCommunicator().sendNormalServerMessage("Age must be 0 or greater than 2, setting minimum but not random.");
                        } else if (age > 100) {
                            age = 100;
                            responder.getCommunicator().sendNormalServerMessage("Age must be 100 or lower, setting maximum.");
                        }
                    } catch (NumberFormatException e) {
                        responder.getCommunicator().sendNormalServerMessage(summoner.getName() + " didn't understand the age, so will select randomly.");
                        age = 0;
                    }
                    byte type;
                    try {
                        type = Byte.parseByte(answers.getProperty("type"));

                        if (!waitingForDetails.allowedTypes.contains(type) && type != 0) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        responder.getCommunicator().sendNormalServerMessage(summoner.getName() + " didn't understand the creature type, so will select normal.");
                        type = 0;
                    }
                    summons.add(new SummonRequest.SummonRequestDetails(waitingForDetails, type, age, amount));
                    unusedOptions.remove(waitingForDetails);
                }
                new BeastSummonerRequestQuestion(this, State.LIST);
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
                                                                       .label(details.nameWithoutAmount)
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

        getResponder().getCommunicator().sendBml(350, 400, true, true, bml, 200, 200, 200, title);
    }

    private void sendOptionQuestion(SummonOption option) {
        List<Map.Entry<Byte, String>> creatureTypes = CreatureTypeList.creatureTypes.stream().filter(it -> option.allowedTypes.contains(it.getKey())).collect(Collectors.toList());
        AtomicInteger i = new AtomicInteger(0);
        String bml = new BMLBuilder(id)
                         .text("Details for - " + option.template.getName())
                         .text("Price per beast - " + getPriceString(option.price))
                         .newLine()
                         .harray(b -> b.label("Amount: ").entry("amount", "1", 3).label("Capped at " + option.cap))
                         .harray(b -> b.label("Age (2-100): ").entry("age", "", 3).label("Blank for random."))
                         .label("Creature type:")
                         .If(creatureTypes.isEmpty(), b -> b.radio("type", "0", "No modifier", true))
                         .forEach(creatureTypes, (creatureType, b) ->
                                      b.radio("type", Byte.toString(creatureType.getKey()), creatureType.getValue(), i.getAndIncrement() == 0))
                         .build();

        getResponder().getCommunicator().sendBml(400, 350, true, true, bml, 200, 200, 200, title);
    }

    private String getPriceString(int price) {
        return profile.acceptsCoin ? new Change(price).getChangeShortString() : String.valueOf(price);
    }

    private String getTypeString(Set<Byte> types) {
        int size = types.size();
        if (size == CreatureTypeList.all.size()) {
            return "Any";
        } else if (size > 1) {
            return "Some";
        } else if (size == 1) {
            return CreatureTypeList.getNameFor(types.iterator().next());
        } else {
            return "None";
        }
    }
}
