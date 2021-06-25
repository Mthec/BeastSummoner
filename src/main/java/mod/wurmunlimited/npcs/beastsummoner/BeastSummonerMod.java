package mod.wurmunlimited.npcs.beastsummoner;

import com.wurmonline.server.behaviours.PlaceBeastSummonerAction;
import com.wurmonline.server.behaviours.PlaceNpcMenu;
import mod.wurmunlimited.npcs.FaceSetter;
import mod.wurmunlimited.npcs.ModelSetter;
import mod.wurmunlimited.npcs.TradeSetup;
import mod.wurmunlimited.npcs.beastsummoner.db.BeastSummonerDatabase;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.Properties;
import java.util.logging.Logger;

public class BeastSummonerMod implements WurmServerMod, Configurable, PreInitable, ServerStartedListener {
    private static final Logger logger = Logger.getLogger(BeastSummonerMod.class.getName());
    public static final int maxTagLength = 25;
    public static final int maxNameLength = 20;
    public static BeastSummonerMod mod;
    public BeastSummonerDatabase db = new BeastSummonerDatabase("beast_summoner.db");
    public FaceSetter faceSetter;
    public ModelSetter modelSetter;
    public static String namePrefix = "Beast_Summoner";

    public BeastSummonerMod() {
        mod = this;
    }

    @Override
    public void configure(Properties properties) {
        namePrefix = properties.getProperty("name_prefix", namePrefix);
    }

    @Override
    public void preInit() {
        TradeSetup.preInit();
        ModActions.init();
    }

    @Override
    public void onServerStarted() {
        new PlaceBeastSummonerAction();
        PlaceNpcMenu.register();
    }
}
