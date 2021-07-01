package com.wurmonline.server.questions;

import com.google.common.base.Joiner;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.Methods;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;
import mod.wurmunlimited.npcs.beastsummoner.db.BeastSummonerDatabase;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import static com.wurmonline.server.creatures.CreaturePackageCaller.saveCreatureName;

public abstract class BeastSummonerPlaceOrManageQuestion extends BeastSummonerQuestionExtension {
    private static final Random r = new Random();
    protected static final ModelOption[] modelOptions = new ModelOption[] { ModelOption.HUMAN, ModelOption.TRADER, ModelOption.CUSTOM };
    protected static final String NO_TAG = "-";
    private final List<String> allTags;
    private final boolean isNew;
    protected final ItemTemplatesDropdown templates = new ItemTemplatesDropdown();
    protected int templateIndex = 0;
    private String filter = "";

    private BeastSummonerPlaceOrManageQuestion(Creature responder, String title, long target, @Nullable Creature summoner) {
        super(responder, title, "", MANAGETRADER, target);
        allTags = BeastSummonerMod.mod.db.getAllTags();
        allTags.add(0, NO_TAG);
        isNew = summoner == null;
    }

    protected BeastSummonerPlaceOrManageQuestion(Creature responder) {
        this(responder, "Set up Beast Summoner", -10, null);
    }

    protected BeastSummonerPlaceOrManageQuestion(Creature responder, Creature summoner) {
        this(responder, "Manage Beast Summoner", summoner.getWurmId(), summoner);
        SummonerProfile profile = BeastSummonerMod.mod.db.getProfileFor(summoner);
        if (profile != null) {
            if (!profile.acceptsCoin) {
                templateIndex = templates.getIndexOf(profile.currency) + 1;
            }
        }
    }

    protected byte getGender() {
        byte sex = 0;
        if (wasAnswered("gender", "female"))
            sex = 1;

        return sex;
    }

    protected String getName(byte sex) {
        return getName(sex, null);
    }

    protected String getName(byte sex, @Nullable Creature summoner) {
        Creature responder = getResponder();

        String name = StringUtilities.raiseFirstLetter(getStringProp("name"));
        if (summoner != null) {
            String oldName = getNameWithoutPrefix(summoner.getName());
            if (name.equals(oldName)) {
                return oldName;
            }
        }
        if (name.isEmpty() || name.length() > 20 || QuestionParser.containsIllegalCharacters(name)) {
            StringBuilder response = new StringBuilder();
            if (summoner != null) {
                response.append(summoner.getName());
            } else {
                response.append("The summoner");
            }

            if (name.isEmpty()) {
                response.append(" chose a new name.");
            } else {
                response.append(" didn't like that name, so chose a new one.");
            }

            if (sex == 0) {
                name = QuestionParser.generateGuardMaleName();
            } else {
                name = QuestionParser.generateGuardFemaleName();

            }

            responder.getCommunicator().sendSafeServerMessage(response.toString());
        }

        return name;
    }

    protected void checkSaveName(Creature summoner) {
        String oldName = summoner.getName();
        String fullName = getPrefix() + getName(summoner.getSex(), summoner);
        if (!fullName.equals(summoner.getName())) {
            try {
                saveCreatureName(summoner, fullName);
                summoner.refreshVisible();
                if (isNew) {
                    getResponder().getCommunicator().sendNormalServerMessage("The summoner will now be known as " + summoner.getName() + ".");
                } else {
                    getResponder().getCommunicator().sendNormalServerMessage(oldName + " will now be known as " + summoner.getName() + ".");
                }
            } catch (IOException e) {
                logger.warning("Failed to set name (" + fullName + ") for creature (" + summoner.getWurmId() + ").");
                getResponder().getCommunicator().sendNormalServerMessage("The summoner looks confused, what exactly is a database?");
                e.printStackTrace();
            }
        }
    }

    protected String getTag() {
        int dropdown = getIntegerOrDefault("tags", 0);
        String tag;

        if (dropdown != 0) {
            tag = allTags.get(dropdown);
            if (tag.equals(NO_TAG))
                tag = "";
        } else {
            tag = getStringProp("tag");
            if (tag.length() > BeastSummonerMod.maxTagLength) {
                getResponder().getCommunicator().sendAlertServerMessage("The tag was too long, so it was cut short.");
                tag = tag.substring(0, BeastSummonerMod.maxTagLength);
            }
        }

        return tag;
    }

    protected void checkSaveTag(Creature summoner, String currentTag) {
        String tag = getTag();

        if (!tag.equals(currentTag)) {
            StringBuilder sb = new StringBuilder();
            sb.append(summoner.getName());

            if (tag.isEmpty())
                sb.append(" was set to use a unique summon list.");
            else
                sb.append("'s tag was set to '").append(tag).append("'.");

            try {
                BeastSummonerMod.mod.db.updateTagFor(summoner, tag);
            } catch (BeastSummonerDatabase.FailedToUpdateTagException e) {
                sb = new StringBuilder(summoner.getName()).append(" looks at the ground and does nothing.");
            }

            getResponder().getCommunicator().sendNormalServerMessage(sb.toString());
        }
    }

    protected boolean doFilter() {
        if (wasSelected("do_filter")) {
            ItemTemplate template = null;
            int index = getPositiveIntegerOrDefault("template", templateIndex);
            if (index != 0) {
                template = templates.getTemplateOrNull(index - 1);
            }

            filter = getStringOrDefault("filter", "");
            templates.filter(filter);
            if (template != null) {
                templateIndex = templates.getIndexOf(template);
            } else {
                templateIndex = 0;
            }

            return true;
        }

        return false;
    }

    protected @Nullable ItemTemplate getCurrencyTemplate() {
        int index = getIntegerOrDefault("template", templateIndex);
        if (index == 0) {
            return null;
        }

        return templates.getTemplateOrNull(index - 1);
    }

    protected void checkSaveCurrency(Creature summoner) {
        ItemTemplate currency = getCurrencyTemplate();
        SummonerProfile profile = BeastSummonerMod.mod.db.getProfileFor(summoner);
        if (profile == null) {
            logger.warning(summoner.getName() + "'s profile was null during checkSaveCurrency.");
            return;
        }
        if (profile.currency != currency) {
            try {
                BeastSummonerMod.mod.db.setCurrencyFor(summoner, currency == null ? -1 : currency.getTemplateId());
                getResponder().getCommunicator().sendNormalServerMessage("The summoner made a note of which currency to use.");
            } catch (SQLException e) {
                logger.warning("Error when updating summoner (" + summoner.getWurmId() + ") currency to " + currency + ".");
                getResponder().getCommunicator().sendNormalServerMessage("The summoner gets lost in thought and forgets about using a different currency.");
            } catch (NoSuchTemplateException e) {
                logger.warning("Template not found when updating summoner (" + summoner.getWurmId() + ") currency to " + currency + ".");
                getResponder().getCommunicator().sendNormalServerMessage("The summoner doesn't understand which currency you want to be used.");
                e.printStackTrace();
            }
        }
    }

    protected boolean locationIsValid(Creature responder, @Nullable VolaTile tile) {
        if (tile != null) {
            if (!Methods.isActionAllowed(responder, Actions.MANAGE_TRADERS)) {
                return false;
            }
            for (Creature creature : tile.getCreatures()) {
                if (!creature.isPlayer()) {
                    responder.getCommunicator().sendNormalServerMessage("The summoner will only set up shop where no other creatures except you are standing.");
                    return false;
                }
            }

            Structure struct = tile.getStructure();
            if (struct != null && !struct.mayPlaceMerchants(responder)) {
                responder.getCommunicator().sendNormalServerMessage("You do not have permission to place a summoner in this building.");
            } else {
                return true;
            }
        }
        return false;
    }

    protected void tryDismiss(Creature summoner) {
        Creature responder = getResponder();
        if (!summoner.isTrading()) {
            Server.getInstance().broadCastAction(summoner.getName() + " grunts, packs " + summoner.getHisHerItsString() + " things and is off.", summoner, 5);
            responder.getCommunicator().sendNormalServerMessage("You dismiss " + summoner.getName() + " from " + summoner.getHisHerItsString() + " post.");
            logger.info(responder.getName() + " dismisses summoner " + summoner.getName() + " with WurmID: " + target);

            try {
                BeastSummonerMod.mod.db.deleteSummoner(summoner);
            } catch (SQLException e) {
                logger.warning("Failed to delete summon options when dismissing summoner (" + summoner.getName() + ").  Some entries may still remain.");
                e.printStackTrace();
            }
            summoner.destroy();
        } else {
            responder.getCommunicator().sendNormalServerMessage(summoner.getName() + " is trading. Try later.");
        }
    }

    // sendQuestion

    protected BML middleBML(BML bml, String namePlaceholder) {
        return bml
                       .harray(b -> b.label("Name: " + getPrefix()).entry("name", namePlaceholder, BeastSummonerMod.maxNameLength))
                       .text("Leave blank for a random name.").italic()
                       .newLine();
    }

    private BML addTagSelector(BML bml, String currentTag) {
        String tagsString = Joiner.on(",").join(allTags);
        return bml
                       .text("Use a 'tag' to use the same summoning options for multiple summoners.")
                       .text("Leave blank to keep the summoning options unique to this summoner.")
                       .harray(b -> b.label("Tag:").entry("tag", currentTag, BeastSummonerMod.maxTagLength))
                       .If(!tagsString.isEmpty(), b -> b
                           .text(" - or - ")
                           .harray(b2 -> b2.dropdown("tags", tagsString).If(!isNew, b3 -> b3.spacer().button("edit", "Edit Tags"))))
                       .newLine()
                       .text("Currency:")
                       .text("Filter available templates:")
                       .text("* is a wildcard that stands in for one or more characters.\ne.g. *clay* to find all clay items or lump* to find all types of lump.")
                       .text("Select the top entry for normal money instead.")
                       .newLine()
                       .harray(b -> b.dropdown("template", "Money," + templates.getTemplatesString(), templateIndex)
                                            .spacer().label("Filter:")
                                            .entry("filter", filter, 10).spacer()
                                            .button("do_filter", "Apply"))
                       .newLine();
    }

    protected String endBML(BML bml) {
        boolean gender = r.nextBoolean();
        return addTagSelector(bml, "")
                       .newLine()
                       .text("Gender:")
                       .radio("gender", "male", "Male", gender)
                       .radio("gender", "female", "Female", !gender)
                       .newLine()
                       .checkbox("customise" ,"Open appearance customiser?", true)
                       .newLine()
                       .harray(b -> b.button("Send"))
                       .newLine()
                       .build();
    }

    protected String endBML(BML bml, String currentTag, Creature summoner) {
        return addTagSelector(bml, currentTag)
                       .harray(b -> b
                            .button("confirm", "Confirm").spacer()
                            .button("list", "Summons List").spacer()
                            .button("customise", "Appearance").spacer()
                            .button("dismiss", "Dismiss").confirm("Dismiss summoner", "Are you sure you wish to dismiss " + summoner.getName() + "?").spacer()
                            .button("cancel", "Cancel").spacer())
                       .newLine()
                       .build();
    }
}
