package mod.wurmunlimited.npcs.beastsummoner;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.WurmObjectsFactory;

public class BeastSummonerObjectsFactory extends WurmObjectsFactory {
    private static boolean createdTemplate = false;

    public BeastSummonerObjectsFactory() throws Exception {
        super();
        if (!createdTemplate) {
            new BeastSummonerTemplate().createCreateTemplateBuilder().build();
            createdTemplate = true;
        }
        BeastSummonerMod.namePrefix = "Beast_Summoner";
    }

    public Creature createNewBeastSummoner() {
        return createNewBeastSummoner(Zones.getOrCreateTile(256, 256, true), -1, -1, "");
    }

    public Creature createNewBeastSummoner(int currency) {
        return createNewBeastSummoner(Zones.getOrCreateTile(256, 256, true), -1, currency, "");
    }

    public Creature createNewBeastSummoner(VolaTile tile, int floorLevel, int currency, String tag) {
        try {
            ItemTemplate currencyTemplate = currency == -1 ? null : ItemTemplateFactory.getInstance().getTemplate(currency);
            Creature summoner = BeastSummonerTemplate.createNewSummoner(tile, floorLevel, randomName("Roger"), (byte)0, Kingdom.KINGDOM_FREEDOM, currencyTemplate, tag);
            creatures.put(summoner.getWurmId(), summoner);
            attachFakeCommunicator(summoner);
            return summoner;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
