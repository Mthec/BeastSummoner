package mod.wurmunlimited.npcs.beastsummoner;

import com.wurmonline.server.creatures.*;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.CreatureCreationQuestion;
import com.wurmonline.server.questions.Question;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BeastSummonerModTests extends BeastSummonerTest {
    private void checkCreatureCreation(Creature gm, InvocationHandler handler, Method method, Object[] args, int templateId) throws Throwable {
        String name = "Name";
        int templateIndex = -1;
        int i = 0;
        //noinspection unchecked
        for (CreatureTemplate template : ((List<CreatureTemplate>)ReflectionUtil.getPrivateField(args[0], CreatureCreationQuestion.class.getDeclaredField("cretemplates")))) {
            if (template.getTemplateId() == templateId) {
                templateIndex = i;
                break;
            }
            ++i;
        }
        assert templateIndex != -1;
        Properties answers = new Properties();
        answers.setProperty("data1", String.valueOf(i));
        answers.setProperty("cname", name);
        answers.setProperty("gender", "female");
        ReflectionUtil.setPrivateField(args[0], Question.class.getDeclaredField("answer"), answers);

        for (Creature creature : factory.getAllCreatures().toArray(new Creature[0])) {
            factory.removeCreature(creature);
        }
        assert factory.getAllCreatures().size() == 0;

        assertNull(handler.invoke(null, method, args));
        verify(method, never()).invoke(null, args);
        assertEquals(1, factory.getAllCreatures().size());
        Creature customTrader = factory.getAllCreatures().iterator().next();
        assertEquals(templateId, customTrader.getTemplateId());
        assertEquals("Beast_Summoner_" + name, customTrader.getName());
        assertEquals((byte)1, customTrader.getSex());
        assertEquals(gm.isOnSurface(), customTrader.isOnSurface());
        assertEquals(0, customTrader.getInventory().getItems().size());
        assertThat(gm, didNotReceiveMessageContaining("An error occurred"));
    }

    @Test
    void testCreatureCreation() throws Throwable {
        Player gm = factory.createNewPlayer();
        Item wand = factory.createNewItem(ItemList.wandGM);
        int tileX = 250;
        int tileY = 250;

        InvocationHandler handler = BeastSummonerMod.mod::creatureCreation;
        Method method = mock(Method.class);
        Object[] args = new Object[] { new CreatureCreationQuestion(gm, "", "", wand.getWurmId(), tileX, tileY, -1, -10) };
        ((CreatureCreationQuestion)args[0]).sendQuestion();

        checkCreatureCreation(gm, handler, method, args, ReflectionUtil.getPrivateField(null, BeastSummonerTemplate.class.getDeclaredField("templateId")));
    }

    @Test
    void testNonCustomTraderCreatureCreation() throws Throwable {
        Player gm = factory.createNewPlayer();
        Item wand = factory.createNewItem(ItemList.wandGM);
        int tileX = 250;
        int tileY = 250;

        InvocationHandler handler = BeastSummonerMod.mod::creatureCreation;
        Method method = mock(Method.class);
        Object[] args = new Object[] { new CreatureCreationQuestion(gm, "", "", wand.getWurmId(), tileX, tileY, -1, -10)};
        ((CreatureCreationQuestion)args[0]).sendQuestion();
        Properties answers = new Properties();
        answers.setProperty("data1", String.valueOf(0));
        answers.setProperty("cname", "MyName");
        answers.setProperty("gender", "female");
        ReflectionUtil.setPrivateField(args[0], Question.class.getDeclaredField("answer"), answers);

        for (Creature creature : factory.getAllCreatures().toArray(new Creature[0])) {
            factory.removeCreature(creature);
        }

        assertNull(handler.invoke(null, method, args));
        verify(method, times(1)).invoke(null, args);
        assertEquals(0, factory.getAllCreatures().size());
        assertThat(gm, didNotReceiveMessageContaining("An error occurred"));
    }

    @Test
    void testDumpTags() throws NoSuchCreatureTemplateException, SQLException {
        BeastSummonerMod.mod.db.addOption(summoner, CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.ANACONDA_CID), 1, 10, Collections.emptySet());
        BeastSummonerMod.mod.db.tagDumpDbString = "sqlite/tags.db";
        assert !new File("sqlite/tags.db").exists();
        assertEquals(MessagePolicy.DISCARD, BeastSummonerMod.mod.onPlayerMessage(gm.getCommunicator(), "/dumptags", ""));
        File file = new File("sqlite/tags.db");
        assertTrue(file.exists());
        //noinspection ResultOfMethodCallIgnored
        file.delete();
        assertThat(gm, receivedMessageContaining("successfully dumped"));
        assertThat(gm, didNotReceiveMessageContaining("need to wait"));

        assertEquals(MessagePolicy.DISCARD, BeastSummonerMod.mod.onPlayerMessage(gm.getCommunicator(), "/dumptags", ""));
        assertThat(gm, receivedMessageContaining("need to wait"));
        assertFalse(new File("sqlite/tags.db").exists());
    }

    @Test
    void testLoadTags() throws NoSuchCreatureTemplateException, SQLException {
        BeastSummonerMod.mod.db.tagDumpDbString = "sqlite/tags.db";
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + BeastSummonerMod.mod.db.tagDumpDbString);
        db.prepareStatement("CREATE TABLE IF NOT EXISTS tag_options (" +
                        "tag TEXT NOT NULL," +
                        "template INTEGER NOT NULL," +
                        "cap INTEGER NOT NULL," +
                        "price INTEGER NOT NULL," +
                        "types TEXT NOT NULL," +
                        "UNIQUE(tag, template));").execute();
        db.close();
        assert new File("sqlite/tags.db").exists();
        assertEquals(MessagePolicy.DISCARD, BeastSummonerMod.mod.onPlayerMessage(gm.getCommunicator(), "/loadtags", ""));
        assertThat(gm, receivedMessageContaining("successfully loaded"));
        assertThat(gm, didNotReceiveMessageContaining("need to wait"));

        assertEquals(MessagePolicy.DISCARD, BeastSummonerMod.mod.onPlayerMessage(gm.getCommunicator(), "/loadtags", ""));
        assertThat(gm, receivedMessageContaining("need to wait"));
    }
}
