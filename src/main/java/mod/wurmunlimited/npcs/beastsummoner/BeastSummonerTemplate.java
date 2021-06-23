package mod.wurmunlimited.npcs.beastsummoner;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.BeastSummonerEconomy;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.shared.constants.CreatureTypes;
import com.wurmonline.shared.constants.ItemMaterials;
import org.gotti.wurmunlimited.modsupport.CreatureTemplateBuilder;
import org.gotti.wurmunlimited.modsupport.creatures.ModCreature;

public class BeastSummonerTemplate implements ModCreature {
    private static int templateId;

    @Override
    public CreatureTemplateBuilder createCreateTemplateBuilder() {
        int[] types = new int[] {
                CreatureTypes.C_TYPE_INVULNERABLE,
                CreatureTypes.C_TYPE_HUMAN
        };

        CreatureTemplateBuilder trader = new CreatureTemplateBuilder(
                "mod.creature.beastsummoner",
                "beast summoner",
                "A grizzled fighter who can summon all manner of beasts, for the right price.",
                "model.creature.humanoid.human.player",
                types,
                (byte)0,
                (short)2,
                MiscConstants.SEX_MALE,
                (short)180,
                (short)20,
                (short)35,
                "sound.death.male",
                "sound.death.female",
                "sound.combat.hit.male",
                "sound.combat.hit.female",
                1.5f,
                2.0f,
                4.0f,
                5f,
                0f,
                0f,
                0.8f,
                0,
                new int[0],
                3,
                0,
                ItemMaterials.MATERIAL_MEAT_HUMAN
        );

        trader.skill(102, 15.0F);
        trader.skill(104, 15.0F);
        trader.skill(103, 10.0F);
        trader.skill(100, 30.0F);
        trader.skill(101, 30.0F);
        trader.skill(105, 99.0F);
        trader.skill(106, 4.0F);
        trader.skill(10052, 40.0F);
        trader.baseCombatRating(70.0f);
        trader.hasHands(true);

        templateId = trader.getTemplateId();

        return trader;
    }

    public static boolean is(Creature creature) {
        return creature.getTemplateId() == templateId;
    }

    public static Creature createNewSummoner(VolaTile tile, int floorLevel, String name, byte sex, byte kingdom, int currency, String tag) throws Exception {
        Creature summoner = Creature.doNew(templateId, (float)(tile.getTileX() << 2) + 2.0F, (float)(tile.getTileY() << 2) + 2.0F, 180.0F, tile.getLayer(), BeastSummonerMod.namePrefix + "_" + name, sex, kingdom);

        if (floorLevel != 0) {
            summoner.pushToFloorLevel(floorLevel);
        }

        BeastSummonerEconomy.createShop(summoner.getWurmId());
        if (currency == -1) {
            BeastSummonerMod.mod.db.addNew(summoner, tile, floorLevel, 0, tag);
        } else {
            BeastSummonerMod.mod.db.addNew(summoner, tile, floorLevel, 0, currency, tag);
        }

        return summoner;
    }
}
