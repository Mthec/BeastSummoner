package mod.wurmunlimited.npcs.beastsummoner;

import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.structures.NoSuchStructureException;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SummonOption {
    private static final Logger logger = Logger.getLogger(SummonOption.class.getName());
    public final CreatureTemplate template;
    public final int gender;
    public final byte kingdom;
    public final int price;
    public final int cap;
    public final Set<Byte> allowedTypes;

    public SummonOption(@NotNull CreatureTemplate template, int price, int cap, Set<Byte> allowedTypes) {
        this.template = template;
        this.price = price;
        this.cap = cap;
        this.gender = template.getSex();
        this.kingdom = 0;
        this.allowedTypes = allowedTypes;
    }

    public void summon(Creature summoner, Player requester, SummonerProfile profile, byte creatureType, int amount, byte age) {
        int actualAmount = Math.min(amount, cap);
        if (actualAmount < amount) {
            logger.warning((amount - actualAmount) + " creatures were not summoned, please report.");
        }

        float posX = (float)((profile.spawnX << 2) + 2);
        float posY = (float)((profile.spawnY << 2) + 2);
        int layer = summoner.getLayer();
        int floorLevel = summoner.getFloorLevel();

        long start = System.nanoTime();
        String nameWithGenus = null;

        for (int i = 0; i < actualAmount; ++i) {
            if (i > 0) {
                posX = profile.getNextPos();
                posY = profile.getNextPos();
            }

            boolean surfaced = summoner.isOnSurface();
            float rot = Server.rand.nextFloat() * 360f;

            try {
                byte sex = 0;
                if (gender == 1 || template.getSex() == 1) {
                    sex = 1;
                }

                long structureId;
                VolaTile tile = Zones.getTileOrNull((int)posX >> 2, (int)posY >> 2, surfaced);
                if (tile == null) {
                    // TODO - Work out if null is strange or normal in this situation.
                    logger.warning("Tile was null for some reason.");
                    structureId = -10;
                } else {
                    Structure structure = tile.getStructure();
                    if (structure != null) {
                        structureId = structure.getWurmId();
                    } else {
                        structureId = -10;
                    }
                }

                long bridgeId = -10L;
                if (structureId > 0L) {
                    try {
                        Structure struct = Structures.getStructure(structureId);
                        if (struct.isTypeBridge()) {
                            bridgeId = structureId;
                        }
                    } catch (NoSuchStructureException ignored) {}
                }

                byte creType = 0;
                if (allowedTypes.contains(creatureType) && (template.hasDen() || template.isRiftCreature())) {
                    creType = creatureType;
                }

                Creature newCreature;
                // !zombie
                if (template.getTemplateId() != 69) {
                    // Not sure why.
                    age -= 1;
                    if (age < 2) {
                        age = (byte)(Server.rand.nextFloat() * 5.0F);
                    }

                    if (template.getTemplateId() != 65 && template.getTemplateId() != 48 && template.getTemplateId() != 98 && template.getTemplateId() != 101 && template.getTemplateId() != 50 && template.getTemplateId() != 117 && template.getTemplateId() != 118) {
                        if (age < 2) {
                            age = (byte)(Server.rand.nextFloat() * (float)Math.min(48, template.getMaxAge()));
                        }

                    }
                    newCreature = Creature.doNew(template.getTemplateId(), true, posX, posY, rot, layer, "", sex, kingdom, creType, false, age, floorLevel);
                } else {
                    newCreature = Creature.doNew(template.getTemplateId(), false, posX, posY, rot, layer, "", sex, kingdom, creType, true, (byte)0, floorLevel);
                }

                if (nameWithGenus == null) {
                    nameWithGenus = newCreature.getNameWithGenus();
                }

                if (structureId > 0L && bridgeId > 0L) {
                    newCreature.setBridgeId(bridgeId);
                }

                if (newCreature.isHorse()) {
                    newCreature.setVisible(false);
                    Creature.setRandomColor(newCreature);
                    newCreature.setVisible(true);
                } else if (newCreature.getTemplate().isColoured) {
                    newCreature.setVisible(false);
                    int randomColour = Server.rand.nextInt(newCreature.getTemplate().maxColourCount);
                    switch (randomColour) {
                        case 1:
                            newCreature.getStatus().setTraitBit(15, true);
                            break;
                        case 2:
                            newCreature.getStatus().setTraitBit(16, true);
                            break;
                        case 3:
                            newCreature.getStatus().setTraitBit(17, true);
                            break;
                        case 4:
                            newCreature.getStatus().setTraitBit(18, true);
                            break;
                        case 5:
                            newCreature.getStatus().setTraitBit(24, true);
                            break;
                        case 6:
                            newCreature.getStatus().setTraitBit(25, true);
                            break;
                        case 7:
                            newCreature.getStatus().setTraitBit(23, true);
                            break;
                        case 8:
                            newCreature.getStatus().setTraitBit(30, true);
                            break;
                        case 9:
                            newCreature.getStatus().setTraitBit(31, true);
                            break;
                        case 10:
                            newCreature.getStatus().setTraitBit(32, true);
                            break;
                        case 11:
                            newCreature.getStatus().setTraitBit(33, true);
                            break;
                        case 12:
                            newCreature.getStatus().setTraitBit(34, true);
                            break;
                        case 13:
                            newCreature.getStatus().setTraitBit(35, true);
                            break;
                        case 14:
                            newCreature.getStatus().setTraitBit(36, true);
                            break;
                        case 15:
                            newCreature.getStatus().setTraitBit(37, true);
                            break;
                        case 16:
                            newCreature.getStatus().setTraitBit(38, true);
                            break;
                    }

                    newCreature.setVisible(true);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, requester.getName() + " asked " + summoner.getName() + " to create creature but failed: " + e.getMessage(), e);
            }
        }

        if (nameWithGenus == null) {
            nameWithGenus = "nothing";
        }

        float elapsedTime = (float)(System.nanoTime() - start) / 1000000.0F;
        logger.info(requester.getName() + " had " + summoner.getName() + " summon " + actualAmount + " " + template.getName() + ", which took " + elapsedTime + " millis.");
        requester.getCommunicator().sendNormalServerMessage("You ask " + summoner.getName() + " to summon " + nameWithGenus + ".");
        Server.getInstance().broadCastAction(requester.getName() + " asks " + summoner.getName() + " to summon beasts and " + nameWithGenus + " quickly appears from nowhere.", requester, profile.range);
    }
}
