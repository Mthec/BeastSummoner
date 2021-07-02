package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.economy.Change;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.creatures.CreatureTypeListCollector;
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
    private final String tag;
    private final List<SummonOption> summons;
    private final String priceSuffix;
    private CreatureTemplatesDropdown dropdown = null;
    private SummonOption editOption = null;
    private final State state;

    BeastSummonerSummonsListQuestion(Creature responder, Creature summoner) {
        super(responder, "Summon List", "", MANAGETRADER, summoner.getWurmId());
        this.summoner = summoner;
        profile = BeastSummonerMod.mod.db.getProfileFor(summoner);
        tag = BeastSummonerMod.mod.db.getTagFor(summoner);
        priceSuffix = profile == null ? "???" : profile.acceptsCoin ? "irons" : profile.currency.getName();
        List<SummonOption> options = BeastSummonerMod.mod.db.getOptionsFor(summoner);
        if (options == null) {
            summons = new ArrayList<>();
        } else {
            summons = new ArrayList<>(options);
        }
        state = State.LIST;
    }

    private BeastSummonerSummonsListQuestion(BeastSummonerSummonsListQuestion oldQuestion, State newState) {
        this(oldQuestion, newState, false);
    }

    private BeastSummonerSummonsListQuestion(BeastSummonerSummonsListQuestion oldQuestion, State newState, boolean filtered) {
        super(oldQuestion.getResponder(), "Summon List", "", MANAGETRADER, oldQuestion.summoner.getWurmId());
        summoner = oldQuestion.summoner;
        profile = oldQuestion.profile;
        tag = oldQuestion.tag;
        summons = oldQuestion.summons;
        priceSuffix = oldQuestion.priceSuffix;
        state = newState;

        switch (state) {
            case LIST:
                sendQuestion();
                break;
            case ADD:
                if (!filtered) {
                    dropdown = new CreatureTemplatesDropdown(summons.stream().map(it -> it.template).collect(Collectors.toList()));
                    sendOptionQuestion(0, 1, 1, new HashSet<>(CreatureTypeList.all));
                } else {
                    dropdown = oldQuestion.dropdown;
                }
                break;
            case EDIT:
                editOption = oldQuestion.editOption;
                if (!filtered) {
                    dropdown = new CreatureTemplatesDropdown(summons.stream().filter(it -> it.template != editOption.template).map(it -> it.template).collect(Collectors.toList()));
                    sendOptionQuestion(dropdown.getIndexOf(editOption.template), editOption.price, editOption.cap, editOption.allowedTypes);
                } else {
                    dropdown = oldQuestion.dropdown;
                }
                break;
        }
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);
        Creature responder = getResponder();

        switch (state) {
            case LIST:
                if (wasSelected("confirm")) {
                    break;
                }

                if (wasSelected("add")) {
                    new BeastSummonerSummonsListQuestion(this, State.ADD);
                    return;
                }

                if (wasSelected("remove")) {
                    for (int i = 0; i < summons.size(); i++) {
                        String val = answers.getProperty("r" + i);
                        if (val != null && val.equals("true")) {
                            try {
                                SummonOption option = summons.remove(i);
                                if (option != null) {
                                    if (tag.isEmpty()) {
                                        BeastSummonerMod.mod.db.deleteOption(summoner, option);
                                    } else {
                                        BeastSummonerMod.mod.db.deleteOption(tag, option);
                                    }
                                }
                            } catch (IndexOutOfBoundsException e) {
                                logger.warning("Attempted to remove summon option outside of range " + i + " - " + summons.size());
                                getResponder().getCommunicator().sendNormalServerMessage("Something went wrong and the summon option was not removed.");
                                break;
                            }
                        }
                    }
                } else if (wasSelected("edit")) {
                    try {
                        int editIndex = Integer.parseInt(answers.getProperty("e", "0"));
                        editOption = summons.get(editIndex);
                        new BeastSummonerSummonsListQuestion(this, State.EDIT);
                        return;
                    } catch (NumberFormatException e) {
                        getResponder().getCommunicator().sendNormalServerMessage("Something went wrong and the summon option was not found.");
                    } catch (IndexOutOfBoundsException e) {
                        logger.warning("Attempted to edit summon option outside of range " + answers.getProperty("e", "none") + " - " + summons.size());
                        getResponder().getCommunicator().sendNormalServerMessage("Something went wrong and the summon option was not found.");
                    }
                }

                new BeastSummonerSummonsListQuestion(this, State.LIST);
                break;
            case ADD:
                if (wasSelected("do_filter")) {
                    parseFilterAndResend(1, 1);
                    break;
                } else if (wasSelected("confirm")) {
                    parseOption(1, 1);
                }

                new BeastSummonerSummonsListQuestion(this, State.LIST);
                break;
            case EDIT:
                if (wasSelected("do_filter")) {
                    parseFilterAndResend(editOption.price, editOption.cap);
                    break;
                } else if (wasSelected("confirm")) {
                    parseOption(editOption.price, editOption.cap);
                }

                new BeastSummonerSummonsListQuestion(this, State.LIST);
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
                    int price = getPositiveIntegerOrDefault("price", -1);
                    if (price == -1) {
                        getResponder().getCommunicator().sendNormalServerMessage("Price must be " + BeastSummonerMod.minimumPrice + " or greater, setting minimum.");
                        price = BeastSummonerMod.minimumPrice;
                    }

                    int cap = getPositiveIntegerOrDefault("cap", -1);
                    if (cap == -1) {
                        getResponder().getCommunicator().sendNormalServerMessage("Cap must be 1 or greater, setting 1.");
                        cap = 1;
                    }

                    try {
                        SummonOption option = BeastSummonerMod.mod.db.updateOption(summoner, editOption, template, price, cap, getAllowedTypes());
                        if (editOption != null) {
                            int idx = summons.indexOf(editOption);
                            if (idx >= 0) {
                                summons.set(idx, option);
                            } else {
                                summons.add(option);
                            }
                        } else {
                            summons.add(option);
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

        new BeastSummonerSummonsListQuestion(this, this.state, true).sendOptionQuestion(templateIndex, price, cap, getAllowedTypes());
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
                             .text("Use this list to set the beasts that will be available to summon.")
                             .newLine()
                             .If(!tag.isEmpty(), b -> b.text("This summoner is using a tag and so any changes will affect all summoners using this tag."))
                             .table(new String[] { "Summon", "Price", "Cap", "Edit", "Remove?" }, summons, (option, b) -> b
                                                          .label(option.template.getName())
                                                          .label(getPriceString(profile, option.price))
                                                          .label(String.valueOf(option.cap))
                                                          .radio("e", String.valueOf(i.get()), "", i.get() == 0)
                                                          .checkbox("r" + i.getAndIncrement()))
                             .button("add", "Add New")
                             .newLine()
                             .harray(b -> b.button("confirm", "Done").spacer().button("edit", "Edit Selected").spacer().button("remove", "Remove Selected"))
                             .build();

        getResponder().getCommunicator().sendBml(350, 400, true, true, bml, 200, 200, 200, title);
    }

    private void sendOptionQuestion(int templateIndex, int price, int cap, Set<Byte> allowedTypes) {
        boolean allSelected = allowedTypes.size() == CreatureTypeList.all.size();
        if (allSelected) {
            allowedTypes = Collections.emptySet();
        }
        final Set<String> finalAllowedTypes = allowedTypes.stream().map(String::valueOf).collect(Collectors.toSet());
        String[][][] creatureTypes = CreatureTypeList.creatureTypes.stream().collect(new CreatureTypeListCollector());

        String bml = new BMLBuilder(id)
                             .text("Select a creature from the dropdown, then fill in the details as necessary.")
                             .newLine()
                             .If(!tag.isEmpty(), b -> b.text("This summoner is using a tag and so any changes will affect all summoners using this tag."))
                             .dropdown("template", dropdown.getTemplatesString(), templateIndex)
                             .newLine()
                             .text("Filter available templates:")
                             .text("* is a wildcard that stands in for one or more characters.\ne.g. *dragon* to find all dragons and hatchlings or Rift* to find all rift creatures.")
                             .newLine()
                             .harray(b -> b.label("Price:")
                                           .entry("price", Integer.toString(price), 10)
                                           .text(priceSuffix))
                             .harray(b -> b.label("Cap: ").entry("cap", Integer.toString(cap), 3))
                             .text("The maximum number players are allowed to summon in one purchase.").italic()
                             .newLine()
                             .label("Creature type modifier (if applicable):")
                             .checkbox("tall", "All (overrides below)", allSelected)
                             .If(true, b -> {
                                 b = b.raw("table{rows=\"" + creatureTypes.length + "\";cols=\"3\";");
                                 for (String[][] line : creatureTypes) {
                                     for (String[] modifier : line) {
                                         if (modifier != null) {
                                             b = b.checkbox("t" + modifier[0], modifier[1], finalAllowedTypes.contains(modifier[0]));
                                         } else {
                                             b.raw(";");
                                         }
                                     }
                                 }
                                 return b.raw("}");
                             })
                             .newLine()
                             .harray(b -> b.button("confirm", "Confirm").spacer().button("cancel", "Cancel"))
                             .build();

        getResponder().getCommunicator().sendBml(350, 400, true, true, bml, 200, 200, 200, title);
    }

    static String getPriceString(SummonerProfile profile, int price) {
        return profile.acceptsCoin ? new Change(price).getChangeShortString() : String.valueOf(price);
    }
}
