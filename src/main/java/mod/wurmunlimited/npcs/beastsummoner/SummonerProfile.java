package mod.wurmunlimited.npcs.beastsummoner;

import com.wurmonline.server.Server;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.zones.VolaTile;
import org.jetbrains.annotations.Nullable;

public class SummonerProfile {
    public final VolaTile spawnPointCentre;
    public final int floorLevel;
    public final int spawnX;
    public final int spawnY;
    public final int range;
    public final float area;
    private final float halfArea;
    public final boolean acceptsCoin;
    public final ItemTemplate currency;

    private SummonerProfile(VolaTile spawnPointCentre, int floorLevel, int range, boolean acceptsCoin, @Nullable ItemTemplate currency) {
        this.spawnPointCentre = spawnPointCentre;
        this.spawnX = spawnPointCentre.getTileX();
        this.spawnY = spawnPointCentre.getTileY();
        this.floorLevel = floorLevel;
        this.range = range;
        float areaFloat = (range << 2) + 2;
        this.area = areaFloat;
        this.halfArea = areaFloat / 2;
        this.acceptsCoin = acceptsCoin;
        this.currency = currency;
    }

    public SummonerProfile(VolaTile spawnPointCentre, int floorLevel, int range) {
        this(spawnPointCentre, floorLevel, range, true, null);
    }

    public SummonerProfile(VolaTile spawnPointCentre, int floorLevel, int range, ItemTemplate currency) {
        this(spawnPointCentre, floorLevel, range, false, currency);
    }

    public float getNextPos() {
        return -halfArea + Server.rand.nextFloat() * area;
    }
}
