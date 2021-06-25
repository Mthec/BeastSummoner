package com.wurmonline.server.questions;

import com.google.common.base.Joiner;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.Methods;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.npcs.FaceSetters;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;
import mod.wurmunlimited.npcs.beastsummoner.db.BeastSummonerDatabase;
import mod.wurmunlimited.npcs.db.FaceSetterDatabase;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import static com.wurmonline.server.creatures.CreaturePackageCaller.saveCreatureName;
import static mod.wurmunlimited.npcs.FaceSetter.FACE_CHANGE_FAILURE;
import static mod.wurmunlimited.npcs.FaceSetter.FACE_CHANGE_SUCCESS;
import static mod.wurmunlimited.npcs.ModelSetter.*;

public abstract class BeastSummonerPlaceOrManageQuestion extends BeastSummonerQuestionExtension {
    private static final Random r = new Random();
    protected static final String NO_TAG = "-";
    private final List<String> allTags;
    private final FaceSetterQuestionHelper face;
    private final ModelSetterQuestionHelper model;
    private final boolean isNew;
    protected Template template;

    private BeastSummonerPlaceOrManageQuestion(Creature responder, String title, long target, @Nullable Creature summoner) {
        super(responder, title, "", MANAGETRADER, target);
        allTags = BeastSummonerMod.mod.db.getAllTags();
        allTags.add(0, NO_TAG);
        face = new FaceSetterQuestionHelper(BeastSummonerMod.mod.faceSetter, summoner);
        model = new ModelSetterQuestionHelper(BeastSummonerMod.mod.modelSetter, summoner, "Trader");
        isNew = summoner == null;
        EligibleTemplates.init();
        template = Template._default();
    }

    protected BeastSummonerPlaceOrManageQuestion(Creature responder, String title, long target) {
        this(responder, title, target, null);
    }

    protected BeastSummonerPlaceOrManageQuestion(Creature responder, String title, Creature summoner) {
        this(responder, title, summoner.getWurmId(), summoner);
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

    protected Creature withTempFace(FaceSetterDatabase.WithTempFace withTempFace, long tempFace) throws Exception {
        return BeastSummonerMod.mod.faceSetter.withTempFace(withTempFace, tempFace);
    }

    private void checkSaveFace(Creature summoner, boolean isHuman) {
        FaceSetterQuestionHelper.FaceResponse response = face.getFace(getAnswer());
        if (response.wasError && !isNew) {
            getResponder().getCommunicator().sendAlertServerMessage("Invalid face value, ignoring.");
        } else if (response.wasRandom) {
            if (response.wasError) {
                getResponder().getCommunicator().sendAlertServerMessage("Invalid face value, setting random.");
            }

            if (isHuman) {
                try {
                    getResponder().getCommunicator().sendCustomizeFace(response.face, BeastSummonerMod.mod.faceSetter.createIdFor(summoner, (Player)getResponder()));
                } catch (FaceSetters.TooManyTransactionsException e) {
                    logger.warning(e.getMessage());
                    getResponder().getCommunicator().sendAlertServerMessage(e.getMessage());
                }
            } else {
                BeastSummonerMod.mod.faceSetter.deleteFaceFor(summoner);
            }
        } else {
            try {
                BeastSummonerMod.mod.faceSetter.setFaceFor(summoner, response.face);
                getResponder().getCommunicator().sendNormalServerMessage(summoner.getName() + FACE_CHANGE_SUCCESS);
            } catch (SQLException e) {
                getResponder().getCommunicator().sendNormalServerMessage(summoner.getName() + FACE_CHANGE_FAILURE);
                logger.warning("Failed to set face (" + response.face + ") for (" + summoner.getWurmId() + ").");
                e.printStackTrace();
            }
        }
    }

    protected void checkSaveModel(Creature summoner) {
        String currentModel = BeastSummonerMod.mod.modelSetter.getModelFor(summoner);
        String modelName = model.getModelName(getAnswer(), TRADER_MODEL_NAME);
        if (!modelName.equals(currentModel)) {
            try {
                BeastSummonerMod.mod.modelSetter.setModelFor(summoner, modelName);
                getResponder().getCommunicator().sendNormalServerMessage(summoner.getName() + MODEL_CHANGE_SUCCESS);
            } catch (SQLException e) {
                getResponder().getCommunicator().sendNormalServerMessage(summoner.getName() + MODEL_CHANGE_FAILURE);
                logger.warning("Failed to set model (" + modelName + ") for (" + summoner.getWurmId() + ").");
                e.printStackTrace();
            }
        }

        checkSaveFace(summoner, modelName.equals(HUMAN_MODEL_NAME));
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
                BeastSummonerMod.mod.db.updateTag(summoner, tag);
            } catch (BeastSummonerDatabase.FailedToUpdateTagException e) {
                sb = new StringBuilder(summoner.getName()).append(" looks at the ground and does nothing.");
            }

            getResponder().getCommunicator().sendNormalServerMessage(sb.toString());
        }
    }

    protected boolean doFilter() {
        if (wasSelected("do_filter")) {
            String filter = getStringOrDefault("filter", "");
            template = new Template(0, filter);

            return true;
        }

        return false;
    }

    protected int getCurrencyIndex() {
        int newTemplateIndex = getIntegerOrDefault("template", template.templateIndex);
        if (newTemplateIndex != template.templateIndex) {
            try {
                template = new Template(template, newTemplateIndex);
            } catch (ArrayIndexOutOfBoundsException ignored) {}
        }

        return newTemplateIndex;
    }

    protected void checkSaveCurrency(Creature summoner) {
        int currency = getCurrencyIndex();
        SummonerProfile profile = BeastSummonerMod.mod.db.getProfileFor(summoner);
        if (profile == null) {
            logger.warning(summoner.getName() + "'s profile was null during checkSaveCurrency.");
            return;
        }
        if (profile.currency != null && profile.currency.getTemplateId() != currency) {
            try {
                BeastSummonerMod.mod.db.setCurrencyFor(summoner, currency);
                getResponder().getCommunicator().sendNormalServerMessage("The summoner made a note of which currency to use.");
            } catch (SQLException e) {
                logger.warning("Error when updating summoner (" + summoner.getWurmId() + ") currency to " + currency + ".");
                getResponder().getCommunicator().sendNormalServerMessage("The summoner gets lost in thought and forgets about using a different currency.");
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
        return model.addQuestion(
                face.addQuestion(bml
                                         .text("Use a 'tag' to use the same summoning options for multiple summoners.")
                                         .text("Leave blank to keep the summoning options unique to this summoner.")
                                         .newLine()
                                         .harray(b -> b.label("Name: " + getPrefix()).entry("name", namePlaceholder, BeastSummonerMod.maxNameLength))
                                         .text("Leave blank for a random name.").italic()
                                         .newLine()));
    }

    private BML addTagSelector(BML bml, String currentTag) {
        return bml
                       .harray(b -> b.label("Tag:").entry("tag", currentTag, BeastSummonerMod.maxTagLength))
                       .text(" - or - ")
                       .harray(b -> b.dropdown("tags", Joiner.on(",").join(allTags)).spacer().button("edit", "Edit Tags"));
    }

    protected String endBML(BML bml) {
        boolean gender = r.nextBoolean();
        return addTagSelector(bml, "")
                       .newLine()
                       .text("Gender:")
                       .radio("gender", "male", "Male", gender)
                       .radio("gender", "female", "Female", !gender)
                       .newLine()
                       .harray(b -> b.button("Send"))
                       .build();
    }

    protected String endBML(BML bml, String currentTag, Creature summoner) {
        return addTagSelector(bml, currentTag)
                       .harray(b -> b
                                            .button("confirm", "Confirm").spacer()
                                            .button("list", "Summons List").spacer()
                                            .button("dismiss", "Dismiss").confirm("Dismiss summoner", "Are you sure you wish to dismiss " + summoner.getName() + "?").spacer()
                                            .button("cancel", "Cancel").spacer())
                       .build();
    }
}
