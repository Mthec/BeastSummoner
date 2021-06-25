package mod.wurmunlimited.npcs;

import com.wurmonline.server.Constants;
import com.wurmonline.server.behaviours.PlaceBeastSummonerAction;
import com.wurmonline.server.behaviours.PlaceNpcMenu;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.ExampleSummonerRequest;
import com.wurmonline.server.questions.SummonRequest;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTemplate;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;
import mod.wurmunlimited.npcs.beastsummoner.db.BeastSummonerDatabase;
import mod.wurmunlimited.npcs.db.Database;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public abstract class BeastSummonerTest {
    private static boolean init = false;
    protected BeastSummonerObjectsFactory factory;
    protected Player gm;
    protected Player player;
    protected Creature summoner;
    protected SummonerProfile profile;
    protected static PlaceNpcMenu menu;

    @BeforeEach
    protected void setUp() throws Exception {
        factory = new BeastSummonerObjectsFactory();

        if (!init) {
            new PlaceBeastSummonerAction();
            menu = PlaceNpcMenu.register();
            init = true;
        }

        ReflectionUtil.<List<FaceSetter>>getPrivateField(null, FaceSetter.class.getDeclaredField("faceSetters")).clear();
        ReflectionUtil.<List<ModelSetter>>getPrivateField(null, ModelSetter.class.getDeclaredField("modelSetters")).clear();

        BeastSummonerMod mod = new BeastSummonerMod();

        mod.faceSetter = new FaceSetter(BeastSummonerTemplate::is, "beast_summoner.db");
        mod.modelSetter = new ModelSetter(BeastSummonerTemplate::is, "beast_summoner.db");

        gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        player = factory.createNewPlayer();
        summoner = factory.createNewBeastSummoner();
        profile = BeastSummonerMod.mod.db.getProfileFor(summoner);
    }

    private static void cleanUp() {
        File file = new File("sqlite/beast_summoner.db");
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        file = new File("sqlite/tags.db");
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        try {
            //noinspection ResultOfMethodCallIgnored
            Files.walk(Paths.get(".")).filter(it -> it.getFileName().toString().startsWith("beast_summoner") && it.getFileName().toString().endsWith("log"))
                    .forEach(it -> it.toFile().delete());

            BeastSummonerMod mod = BeastSummonerMod.mod;
            if (mod != null) {
                BeastSummonerDatabase db = mod.db;
                if (db != null) {
                    ReflectionUtil.setPrivateField(db, Database.class.getDeclaredField("created"), false);
                }
            }
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void reset() {
        cleanUp();
        Constants.dbHost = ".";
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    protected SummonRequest exampleSummon() {
        return ExampleSummonerRequest.exampleSummon(summoner, player, profile);
    }

    protected static void execute(Database.Execute execute) {
        try {
            ReflectionUtil.callPrivateMethod(null, Database.class.getDeclaredMethod("execute", Database.Execute.class), execute);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
