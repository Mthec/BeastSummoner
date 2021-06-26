package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.economy.Change;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;
import mod.wurmunlimited.npcs.beastsummoner.SummonOption;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BeastSummonerSummonsListQuestion extends BeastSummonerQuestionExtension {
    private enum State {
        LIST, ADD, EDIT
    }

    private final Creature summoner;
    private final SummonerProfile profile;
    private final List<SummonOption> summons;
    private final String priceSuffix;
    private CreatureTemplatesDropdown dropdown = null;
    private SummonOption editOption = null;
    private State state = State.LIST;

    BeastSummonerSummonsListQuestion(Creature responder, Creature summoner) {
        super(responder, "Summon List", "", MANAGETRADER, summoner.getWurmId());
        this.summoner = summoner;
        profile = BeastSummonerMod.mod.db.getProfileFor(summoner);
        priceSuffix = profile == null ? "???" : profile.acceptsCoin ? "irons" : profile.currency.getName();
        List<SummonOption> options = BeastSummonerMod.mod.db.getOptionsFor(summoner);
        if (options == null) {
            summons = new ArrayList<>();
        } else {
            summons = options;
        }
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);
        Creature responder = getResponder();

        switch (state) {
            case LIST:
                if (wasSelected("submit")) {
                    break;
                }

                if (wasSelected("add")) {
                    state = State.ADD;
                    dropdown = new CreatureTemplatesDropdown(summons.stream().map(it -> it.template).collect(Collectors.toList()));
                    sendOptionQuestion(0, 1, 1, new HashSet<>(CreatureTypeList.all));
                    break;
                }

                for (int i = 0; i < summons.size(); i++) {
                    String property = answers.getProperty("r" + i);
                    if (property != null && property.equals("true")) {
                        summons.remove(i);
                        break;
                    }

                    property = answers.getProperty("e" + i);
                    if (property != null && property.equals("true")) {
                        state = State.EDIT;
                        dropdown = new CreatureTemplatesDropdown(summons.stream().map(it -> it.template).collect(Collectors.toList()));
                        editOption = summons.get(i);
                        sendOptionQuestion(dropdown.getIndexOf(editOption.template), editOption.price, editOption.cap, editOption.allowedTypes);
                        return;
                    }
                }

                sendQuestion();
                break;
            case ADD:
                if (wasSelected("do_filter")) {
                    parseFilterAndResend(1, 1);
                    break;
                } else if (wasSelected("submit")) {
                    parseOption(1, 1);
                }

                state = State.LIST;
                sendQuestion();
                break;
            case EDIT:
                if (wasSelected("do_filter")) {
                    parseFilterAndResend(editOption.price, editOption.cap);
                    break;
                } else if (wasSelected("submit")) {
                    parseOption(editOption.price, editOption.cap);
                }

                editOption = null;
                state = State.LIST;
                sendQuestion();
                break;
        }
    }

    private Set<Byte> getAllowedTypes() {
        if (getStringOrDefault("tall", "").equals("true")) {
            return new HashSet<>(CreatureTypeList.all);
        }

        Properties answers = getAnswer();
        Set<Byte> types = new HashSet<>();
        for (Byte type : CreatureTypeList.all) {
            String val = answers.getProperty("t" + type);
            if (val != null && val.equals("true")) {
                types.add(type);
            }
        }

        return types;
    }

    private void parseOption(int defaultPrice, int defaultCap) {
        String indexString = getAnswer().getProperty("template");
        if (indexString != null && !indexString.isEmpty()) {
            try {
                int index = Integer.parseInt(indexString);
                CreatureTemplate template = dropdown.getTemplateOrNull(index);
                if (template != null) {
                    int price = getPositiveIntegerOrDefault("price", 1);
                    int cap = getPositiveIntegerOrDefault("cap", 1);

                    try {
                        SummonOption option = BeastSummonerMod.mod.db.updateOption(summoner, editOption, template, price, cap, getAllowedTypes());
                        if (option != null) {
                            int indexOfOldOption = summons.indexOf(editOption);

                            if (indexOfOldOption >= 0) {
                                summons.set(indexOfOldOption, option);
                            } else {
                                summons.add(option);
                            }
                        }
                    } catch (SQLException e) {
                        getResponder().getCommunicator().sendNormalServerMessage("Something went wrong and the summon option was not saved.");
                        logger.warning("Error saving summon option with (" + price + ", " + cap + ").");
                        e.printStackTrace();
                    }
                } else {
                    throw new NumberFormatException("Invalid index.");
                }
            } catch (NumberFormatException ignored) {
                getResponder().getCommunicator().sendNormalServerMessage("That template could not be found.");
            }
        }
    }

    private void parseFilterAndResend(int defaultPrice, int defaultCap) {
        int templateIndex = 0;
        CreatureTemplate template = null;
        int index = getPositiveIntegerOrDefault("template", templateIndex);
        if (index != 0) {
            template = dropdown.getTemplateOrNull(index - 1);
        }

        dropdown.filter(getStringOrDefault("filter", ""));
        if (template != null) {
            templateIndex = dropdown.getIndexOf(template);
        }

        int price = getPositiveIntegerOrDefault("price", defaultPrice);
        int cap = getPositiveIntegerOrDefault("cap", defaultCap);

        sendOptionQuestion(templateIndex, price, cap, getAllowedTypes());
    }

    @Override
    public void sendQuestion() {
        if (profile == null) {
            logger.warning("Profile was null for " + summoner.getName() + "(" + summoner.getWurmId() + ").");
            getResponder().getCommunicator().sendAlertServerMessage("Something went wrong in the mists of the void, and the summoner details could not be found.");
            return;
        }

        AtomicInteger i = new AtomicInteger(0);
        String bml = new BMLBuilder(id)
                             .table(new String[] { "Summon", "Price", "Cap", "Edit", "Remove?" }, summons, (option, b) -> b
                                                          .label(option.template.getName())
                                                          .label(getPriceString(profile, option.price))
                                                          .label(String.valueOf(option.cap))
                                                          .button("e" + i.getAndIncrement(), "?")
                                                          .button("r" + (i.get() - 1), "x"))
                             .button("add", "Add")
                             .newLine()
                             .harray(b -> b.button("submit", "Send"))
                             .build();
    }

    private void sendOptionQuestion(int templateIndex, int price, int cap, Set<Byte> allowedTypes) {
        boolean allSelected = allowedTypes.size() == CreatureTypeList.all.size();
        if (allSelected) {
            allowedTypes = Collections.emptySet();
        }
        final Set<Byte> finalAllowedTypes = allowedTypes;

        String bml = new BMLBuilder(id)
                             .dropdown("template", dropdown.getTemplatesString(), templateIndex)
                             .newLine()
                             .harray(b -> b.label("Price").spacer()
                                           .entry("price", Integer.toString(price), 10).spacer()
                                           .text(priceSuffix))
                             .harray(b -> b.label("Cap: ").entry("cap", Integer.toString(cap), 3))
                             .text("The maximum number players are allowed to summon in one purchase.").italic()
                             .newLine()
                             .label("Creature type modifier (if applicable):")
                             .checkbox("tall", "All (overrides below)", allSelected)
                             .forEach(CreatureTypeList.creatureTypes, (creatureType, b) -> b.checkbox("t" + creatureType.getKey(), creatureType.getValue(), finalAllowedTypes.contains(creatureType.getKey())))
                             .newLine()
                             .harray(b -> b.button("submit", "Confirm").spacer().button("cancel", "Cancel"))
                             .build();

        getResponder().getCommunicator().sendBml(350, 200, true, true, bml, 200, 200, 200, title);
    }

    static String getPriceString(SummonerProfile profile, int price) {
        return profile.acceptsCoin ? new Change(price).getChangeShortString() : String.valueOf(price);
    }
}
