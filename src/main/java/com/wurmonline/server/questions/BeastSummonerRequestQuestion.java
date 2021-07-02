package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.BeastSummonerTradeHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.items.BeastSummonerTrade;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.creaturecustomiser.CreatureTypeListCollector;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;
import mod.wurmunlimited.npcs.beastsummoner.SummonOption;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
                } else if (wasSelected("confirm")) {
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
                        String ageString = answers.getProperty("age");
                        if (ageString == null) {
                            throw new NumberFormatException();
                        } else if (ageString.isEmpty()) {
                            age = 0;
                        } else {
                            age = Byte.parseByte(ageString);
                            int maxAge = waitingForDetails.template.getMaxAge();
                            if (age < 0) {
                                age = 0;
                                responder.getCommunicator().sendNormalServerMessage("Age must be 0 or greater than 2, so it will be selected randomly.");
                            } else if (age == 1) {
                                age = 2;
                                responder.getCommunicator().sendNormalServerMessage("Age must be 0 or greater than 2, setting minimum but not random.");
                            } else if (age > maxAge) {
                                age = (byte)maxAge;
                                responder.getCommunicator().sendNormalServerMessage("Age for this beast must be " + maxAge + " or lower, setting maximum.");
                            }
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
        if (profile == null) {
            logger.warning("Profile was null for " + summoner.getName() + "(" + summoner.getWurmId() + ").");
            getResponder().getCommunicator().sendAlertServerMessage("Something went wrong in the mists of the void, and the summoner details could not be found.");
            return;
        }

        AtomicInteger i = new AtomicInteger(0);
        String bml;
        if (unusedOptions.isEmpty() && summons.isEmpty()) {
            bml = new BMLBuilder(id)
                .text("This summoner is not currently able to summon any beasts.")
                .newLine()
                .harray(b -> b.button("cancel", "Send"))
                .build();
        } else {
            bml = new BMLBuilder(id)
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
                .harray(b -> b.button("confirm", "Send").spacer().button("cancel", "Cancel"))
                .build();
        }

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
                                                         .label(getTypeString(option))
                                                         .button("a" + i.getAndIncrement(), "Add"))
                             .build();

        getResponder().getCommunicator().sendBml(350, 400, true, true, bml, 200, 200, 200, title);
    }

    private void sendOptionQuestion(SummonOption option) {
        String[][][] creatureTypes = CreatureTypeList.creatureTypes.stream()
                                                              .filter(it -> option.allowedTypes.contains(it.first))
                                                              .collect(new CreatureTypeListCollector());
        int maxAge = option.template.getMaxAge();
        AtomicInteger i = new AtomicInteger(0);
        String bml = new BMLBuilder(id)
                         .text("Details for - " + option.template.getName())
                         .text("Price per beast - " + getPriceString(option.price))
                         .newLine()
                         .harray(b -> b.label("Amount: ").entry("amount", "1", 3).label("Capped at " + option.cap))
                         .harray(b -> b.label("Age (" + (maxAge == 2 ? "2" : "2-" + maxAge) + "): ").entry("age", "", 3).label("Blank for random."))
                         .label("Creature type:")
                         .If(creatureTypes.length == 0 || (!option.template.hasDen() && !option.template.isRiftCreature()),
                                 b -> b.radio("type", "0", "No modifier", true),
                                 b -> {
                                     b = b.raw("table{rows=\"" + creatureTypes.length + "\";cols=\"3\";");
                                     for (String[][] line : creatureTypes) {
                                         for (String[] modifier : line) {
                                             if (modifier != null) {
                                                 b = b.radio("type", modifier[0], modifier[1], i.getAndIncrement() == 0);
                                             } else {
                                                 b.raw(";");
                                             }
                                         }
                                     }
                                     return b.raw("}");
                                 })
                         .newLine()
                         .harray(b -> b.button("confirm", "Add").spacer().button("cancel", "Cancel"))
                         .newLine()
                         .build();

        getResponder().getCommunicator().sendBml(300, 350, true, true, bml, 200, 200, 200, title);
    }

    private String getPriceString(int price) {
        return profile.acceptsCoin ? new Change(price).getChangeShortString() : String.valueOf(price);
    }

    private String getTypeString(SummonOption option) {
        if (!option.template.hasDen() && !option.template.isRiftCreature()) {
            return "None";
        }
        int size = option.allowedTypes.size();
        if (size == CreatureTypeList.all.size()) {
            return "Any";
        } else if (size > 1) {
            return "Some";
        } else if (size == 1) {
            return CreatureTypeList.getNameFor(option.allowedTypes.iterator().next());
        } else {
            return "None";
        }
    }
}
