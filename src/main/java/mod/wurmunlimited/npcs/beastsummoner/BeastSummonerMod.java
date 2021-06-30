package mod.wurmunlimited.npcs.beastsummoner;

import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.BeastSummonerTradeHandler;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.CreatureCreationQuestion;
import com.wurmonline.server.questions.PlaceBeastSummonerQuestion;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.npcs.DestroyHandler;
import mod.wurmunlimited.npcs.FaceSetter;
import mod.wurmunlimited.npcs.ModelSetter;
import mod.wurmunlimited.npcs.TradeSetup;
import mod.wurmunlimited.npcs.beastsummoner.db.BeastSummonerDatabase;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.gotti.wurmunlimited.modsupport.creatures.ModCreatures;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class BeastSummonerMod implements WurmServerMod, Configurable, Initable, PlayerMessageListener, PreInitable, ServerStartedListener {
    private static final Logger logger = Logger.getLogger(BeastSummonerMod.class.getName());
    public static final int maxTagLength = 25;
    public static final int maxNameLength = 20;
    private static final String dbName = "beast_summoner.db";
    public static BeastSummonerMod mod;
    public final BeastSummonerDatabase db = new BeastSummonerDatabase(dbName);
    public FaceSetter faceSetter;
    public ModelSetter modelSetter;
    public static String namePrefix = "Beast_Summoner";
    private final CommandWaitTimer dumpLoadTagsTimer = new CommandWaitTimer(TimeConstants.MINUTE_MILLIS / 2);

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
    public void init() {
        HookManager manager = HookManager.getInstance();

        manager.registerHook("com.wurmonline.server.questions.QuestionParser",
                "parseCreatureCreationQuestion",
                "(Lcom/wurmonline/server/questions/CreatureCreationQuestion;)V",
                () -> this::creatureCreation);

        TradeSetup.init(manager);
        FaceSetter.init(manager);
        ModelSetter.init(manager);
        DestroyHandler.addListener(creature -> {
            try {
                db.deleteSummoner((Creature)creature);
            } catch (SQLException e) {
                logger.warning("Error when deleting Beast Summoner.");
                e.printStackTrace();
            }
        });
        ModCreatures.init();
        ModCreatures.addCreature(new BeastSummonerTemplate());
    }

    @Override
    public void onServerStarted() {
        db.loadData();
        TradeSetup.addTrader(BeastSummonerTemplate::is, BeastSummonerTradeHandler::create);
        faceSetter = new FaceSetter(BeastSummonerTemplate::is, dbName);
        modelSetter = new ModelSetter(BeastSummonerTemplate::is, dbName);

        ModActions.registerAction(new ManageBeastSummonerAction());
        ModActions.registerAction(new StartSetSpawnAction());
        ModActions.registerAction(new SetSpawnAction());
        ModActions.registerAction(new CancelSetSpawnAction());
        ModActions.registerAction(new RequestAction());
        new PlaceBeastSummonerAction();
        PlaceNpcMenu.register();
    }

    Object creatureCreation(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        CreatureCreationQuestion question = (CreatureCreationQuestion)args[0];
        Properties answers = ReflectionUtil.getPrivateField(question, Question.class.getDeclaredField("answer"));
        try {
            String templateIndexString = answers.getProperty("data1");
            String name = answers.getProperty("cname");
            if (name == null)
                answers.setProperty("name", "");
            else
                answers.setProperty("name", name);
            if (templateIndexString != null) {
                int templateIndex = Integer.parseInt(templateIndexString);
                List<CreatureTemplate> templates = ReflectionUtil.getPrivateField(question, CreatureCreationQuestion.class.getDeclaredField("cretemplates"));
                CreatureTemplate template = templates.get(templateIndex);

                Creature responder = question.getResponder();
                int floorLevel = responder.getFloorLevel();
                VolaTile tile = Zones.getOrCreateTile(question.getTileX(), question.getTileY(), responder.isOnSurface());
                if (BeastSummonerTemplate.is(template)) {
                    new PlaceBeastSummonerQuestion(responder, tile, floorLevel).answer(answers);
                    return null;
                }
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            question.getResponder().getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The summoner was not created.");
            e.printStackTrace();
        }

        return method.invoke(o, args);
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        Player player = communicator.getPlayer();

        if (player != null && player.getPower() >= 2) {
            if (message.equals("/dumptags")) {
                String timeRemaining = dumpLoadTagsTimer.timeRemaining();
                if (timeRemaining.isEmpty()) {
                    try {
                        db.dumpTags();
                        communicator.sendSafeServerMessage("Beast Summoner tags were successfully dumped.");
                    } catch (SQLException e) {
                        communicator.sendAlertServerMessage("An error occurred when dumping tags.");
                        e.printStackTrace();
                    }

                    dumpLoadTagsTimer.reset();
                } else {
                    player.getCommunicator().sendNormalServerMessage("You need to wait " + timeRemaining + " before /dumptags or /loadtags can be used again.");
                }

                return MessagePolicy.DISCARD;
            } else if (message.equals("/loadtags")) {
                String timeRemaining = dumpLoadTagsTimer.timeRemaining();
                if (timeRemaining.isEmpty()) {
                    try {
                        db.loadTags();
                        communicator.sendSafeServerMessage("Beast Summoner tags were successfully loaded.");
                    } catch (SQLException e) {
                        communicator.sendAlertServerMessage("An error occurred when loading tags.");
                        e.printStackTrace();
                    }

                    dumpLoadTagsTimer.reset();
                } else {
                    player.getCommunicator().sendNormalServerMessage("You need to wait " + timeRemaining + " before /dumptags or /loadtags can be used again.");
                }

                return MessagePolicy.DISCARD;
            }
        }

        return MessagePolicy.PASS;
    }

    @Override
    public boolean onPlayerMessage(Communicator communicator, String s) {
        return false;
    }
}
