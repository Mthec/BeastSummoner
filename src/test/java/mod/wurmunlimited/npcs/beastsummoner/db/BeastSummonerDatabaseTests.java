package mod.wurmunlimited.npcs.beastsummoner.db;

import com.wurmonline.server.creatures.*;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.questions.CreatureTypeList;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTemplate;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;
import mod.wurmunlimited.npcs.beastsummoner.SummonOption;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BeastSummonerDatabaseTests extends BeastSummonerTest {
    private final String dbName = "sqlite/beast_summoner.db";

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        db.tagDumpDbString = "sqlite/tags.db";
    }

    private void createOptionEntries(String path, CreatureTemplate template1, CreatureTemplate template2, int n) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path)) {
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS tag_options (" +
                                        "tag TEXT NOT NULL," +
                                        "template INTEGER NOT NULL," +
                                        "cap INTEGER NOT NULL," +
                                        "price INTEGER NOT NULL," +
                                        "types TEXT NOT NULL," +
                                        "UNIQUE(tag, template));").execute();
            PreparedStatement ps = conn.prepareStatement("INSERT INTO tag_options (tag, template, cap, price, types) VALUES (?, ?, ?, ?, ?);");
            String tag = db.getTagFor(summoner);
            ps.setString(1, tag);
            ps.setInt(2, template1.getTemplateId());
            ps.setInt(3, n);
            ps.setInt(4, n + 1);
            ps.setString(5, "all");
            ps.execute();
            ps.setString(1, tag);
            ps.setInt(2, template2.getTemplateId());
            ps.setInt(3, n + 1);
            ps.setInt(4, n);
            ps.setString(5, "all");
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLoadTags() throws SQLException, NoSuchCreatureTemplateException, BeastSummonerDatabase.FailedToUpdateTagException {
        int n = 5;
        CreatureTemplate template1 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.CHICKEN_CID);
        CreatureTemplate template2 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.DEER_CID);
        db.tagDumpDbString = "sqlite/tags.db";
        db.updateTagFor(summoner, "tag");
        createOptionEntries(db.tagDumpDbString, template1, template2, n);

        assert db.getOptionsFor(summoner) == null;

        db.loadTags();
        List<SummonOption> options = db.getOptionsFor(summoner);
        assertNotNull(options);
        assertEquals(2, options.size());
        SummonOption option1 = options.get(0);
        SummonOption option2 = options.get(1);
        assertEquals(template1, option1.template);
        assertEquals(n, option1.cap);
        assertEquals(n + 1, option1.price);
        assertEquals(CreatureTypeList.all.size(), option1.allowedTypes.size());
        assertEquals(template2, option2.template);
        assertEquals(n + 1, option2.cap);
        assertEquals(n, option2.price);
        assertEquals(CreatureTypeList.all.size(), option2.allowedTypes.size());
    }

    @Test
    void testDumpTags() throws SQLException, NoSuchCreatureTemplateException, BeastSummonerDatabase.FailedToUpdateTagException {
        String tag = "myTag";
        int n = 5;
        CreatureTemplate template1 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.PIG_CID);
        CreatureTemplate template2 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.DRAKESPIRIT_CID);
        db.updateTagFor(summoner, tag);
        createOptionEntries(dbName, template1, template2, n);
        db.loadData();
        assert Objects.requireNonNull(db.getOptionsFor(summoner)).size() == 2;

        db.dumpTags();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db.tagDumpDbString)) {
            ResultSet rs = conn.prepareStatement("SELECT * FROM tag_options;").executeQuery();

            assertTrue(rs.next());
            assertEquals(tag, rs.getString(1));
            assertEquals(template1.getTemplateId(), rs.getInt(2));
            assertEquals(n, rs.getInt(3));
            assertEquals(n + 1, rs.getInt(4));
            assertEquals("all", rs.getString(5));
            assertTrue(rs.next());
            assertEquals(tag, rs.getString(1));
            assertEquals(template2.getTemplateId(), rs.getInt(2));
            assertEquals(n + 1, rs.getInt(3));
            assertEquals(n, rs.getInt(4));
            assertEquals("all", rs.getString(5));
        }
    }

    private void clearDb() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbName)) {
            conn.prepareStatement("DROP TABLE IF EXISTS summoner;DROP TABLE IF EXISTS options;DROP TABLE IF EXISTS tag_options;").execute();
            ReflectionUtil.<Map<Creature, SummonerProfile>>getPrivateField(db, BeastSummonerDatabase.class.getDeclaredField("allProfiles")).clear();
            ReflectionUtil.<Map<Creature, String>>getPrivateField(db, BeastSummonerDatabase.class.getDeclaredField("allTags")).clear();
            ReflectionUtil.<Map<Creature, List<SummonOption>>>getPrivateField(db, BeastSummonerDatabase.class.getDeclaredField("allOptions")).clear();
            ReflectionUtil.<Map<String, List<SummonOption>>>getPrivateField(db, BeastSummonerDatabase.class.getDeclaredField("allTagOptions")).clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createProfile(int range, int currency, String tag) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbName)) {
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS summoner (" +
                                        "id INTEGER NOT NULL UNIQUE," +
                                        "x INTEGER NOT NULL," +
                                        "y INTEGER NOT NULL," +
                                        "surfaced INTEGER NOT NULL," +
                                        "range INTEGER NOT NULL," +
                                        "floor_level INTEGER NOT NULL," +
                                        "currency INTEGER NOT NULL," +
                                        "tag TEXT NOT NULL" +
                                        ");").execute();
            PreparedStatement ps = conn.prepareStatement("INSERT INTO summoner (id, x, y, surfaced, floor_level, range, currency, tag) VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
            ps.setLong(1, summoner.getWurmId());
            ps.setInt(2, 123);
            ps.setInt(3, 256);
            ps.setBoolean(4, true);
            ps.setInt(5, -1);
            ps.setInt(6, range);
            ps.setInt(7, currency);
            ps.setString(8, tag);
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createOptionUnique(CreatureTemplate template, int n) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbName)) {
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS options (" +
                                          "id INTEGER NOT NULL," +
                                          "template INTEGER NOT NULL," +
                                          "cap INTEGER NOT NULL," +
                                          "price INTEGER NOT NULL," +
                                          "types TEXT NOT NULL," +
                                          "UNIQUE(id, template));").execute();
            PreparedStatement ps = conn.prepareStatement("INSERT INTO options (id, template, cap, price, types) VALUES (?, ?, ?, ?, ?);");
            ps.setLong(1, summoner.getWurmId());
            ps.setInt(2, template.getTemplateId());
            ps.setInt(3, n);
            ps.setInt(4, n + 1);
            ps.setString(5, "all");
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createOptionTag(String tag, CreatureTemplate template, int n) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbName)) {
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS tag_options (" +
                                          "tag TEXT NOT NULL," +
                                          "template INTEGER NOT NULL," +
                                          "cap INTEGER NOT NULL," +
                                          "price INTEGER NOT NULL," +
                                          "types TEXT NOT NULL," +
                                          "UNIQUE(tag, template));").execute();
            PreparedStatement ps = conn.prepareStatement("INSERT INTO tag_options (tag, template, cap, price, types) VALUES (?, ?, ?, ?, ?);");
            ps.setString(1, tag);
            ps.setInt(2, template.getTemplateId());
            ps.setInt(3, n);
            ps.setInt(4, n + 1);
            ps.setString(5, "all");
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLoadData() throws NoSuchCreatureTemplateException, BeastSummonerDatabase.FailedToUpdateTagException {
        int n = 7;
        String tag = "test";
        clearDb();
        CreatureTemplate template1 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.CROCODILE_CID);
        CreatureTemplate template2 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.SHEEP_CID);
        createProfile(n, -1, tag);
        createOptionUnique(template1, n);
        createOptionTag(tag, template2, n + 2);
        db.loadData();

        SummonerProfile profile = db.getProfileFor(summoner);
        assertNotNull(profile);
        assertEquals(123, profile.spawnX);
        assertEquals(256, profile.spawnY);
        assertTrue(profile.spawnPointCentre.isOnSurface());
        assertEquals(-1, profile.floorLevel);
        assertEquals(n, profile.range);
        assertTrue(profile.acceptsCoin);

        SummonOption option2 = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);
        assertEquals(template2, option2.template);
        assertEquals(n + 2, option2.cap);
        assertEquals(n + 3, option2.price);
        assertEquals(CreatureTypeList.all.size(), option2.allowedTypes.size());

        db.updateTagFor(summoner, "");
        SummonOption option1 = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);
        assertEquals(template1, option1.template);
        assertEquals(n, option1.cap);
        assertEquals(n + 1, option1.price);
        assertEquals(CreatureTypeList.all.size(), option1.allowedTypes.size());
    }

    @Test
    void testAddNewWithCurrency() throws NoSuchFieldException, IllegalAccessException, NoSuchTemplateException, SQLException {
        int n = 10;
        ItemTemplate currency = ItemTemplateFactory.getInstance().getTemplate(ItemList.lunchbox);
        String tag = "anotherTag" + n;
        Creature newSummoner = factory.createNewCreature(ReflectionUtil.<Integer>getPrivateField(null, BeastSummonerTemplate.class.getDeclaredField("templateId")));
        db.addNew(newSummoner, gm.getCurrentTile(), n, n + 1, currency, tag);

        SummonerProfile profile = db.getProfileFor(newSummoner);
        assertNotNull(profile);
        assertEquals(gm.getCurrentTile().getTileX(), profile.spawnX);
        assertEquals(gm.getCurrentTile().getTileY(), profile.spawnY);
        assertEquals(gm.getCurrentTile().isOnSurface(), profile.spawnPointCentre.isOnSurface());
        assertEquals(n, profile.floorLevel);
        assertEquals(n + 1, profile.range);
        assertFalse(profile.acceptsCoin);
        assertEquals(currency, profile.currency);
        assertEquals(tag, db.getTagFor(newSummoner));

        db.loadData();
        profile = db.getProfileFor(newSummoner);
        assertNotNull(profile);
        assertEquals(gm.getCurrentTile().getTileX(), profile.spawnX);
        assertEquals(gm.getCurrentTile().getTileY(), profile.spawnY);
        assertEquals(gm.getCurrentTile().isOnSurface(), profile.spawnPointCentre.isOnSurface());
        assertEquals(n, profile.floorLevel);
        assertEquals(n + 1, profile.range);
        assertFalse(profile.acceptsCoin);
        assertEquals(currency, profile.currency);
        assertEquals(tag, db.getTagFor(newSummoner));
    }

    @Test
    void testAddNewWithCoin() throws NoSuchFieldException, IllegalAccessException, NoSuchTemplateException, SQLException {
        int n = 11;
        String tag = "anotherTag" + n;
        Creature newSummoner = factory.createNewCreature(ReflectionUtil.<Integer>getPrivateField(null, BeastSummonerTemplate.class.getDeclaredField("templateId")));
        db.addNew(newSummoner, gm.getCurrentTile(), n, n + 1, tag);

        SummonerProfile profile = db.getProfileFor(newSummoner);
        assertNotNull(profile);
        assertEquals(gm.getCurrentTile().getTileX(), profile.spawnX);
        assertEquals(gm.getCurrentTile().getTileY(), profile.spawnY);
        assertEquals(gm.getCurrentTile().isOnSurface(), profile.spawnPointCentre.isOnSurface());
        assertEquals(n, profile.floorLevel);
        assertEquals(n + 1, profile.range);
        assertTrue(profile.acceptsCoin);
        assertNull(profile.currency);
        assertEquals(tag, db.getTagFor(newSummoner));

        db.loadData();
        profile = db.getProfileFor(newSummoner);
        assertNotNull(profile);
        assertEquals(gm.getCurrentTile().getTileX(), profile.spawnX);
        assertEquals(gm.getCurrentTile().getTileY(), profile.spawnY);
        assertEquals(gm.getCurrentTile().isOnSurface(), profile.spawnPointCentre.isOnSurface());
        assertEquals(n, profile.floorLevel);
        assertEquals(n + 1, profile.range);
        assertTrue(profile.acceptsCoin);
        assertNull(profile.currency);
        assertEquals(tag, db.getTagFor(newSummoner));
    }

    @Test
    void testAddNewWithoutTag() throws NoSuchFieldException, IllegalAccessException, SQLException {
        int n = 11;
        Creature newSummoner = factory.createNewCreature(ReflectionUtil.<Integer>getPrivateField(null, BeastSummonerTemplate.class.getDeclaredField("templateId")));
        db.addNew(newSummoner, gm.getCurrentTile(), n, n + 1, "");

        SummonerProfile profile = db.getProfileFor(newSummoner);
        assertNotNull(profile);
        assertEquals("", db.getTagFor(newSummoner));

        db.loadData();
        profile = db.getProfileFor(newSummoner);
        assertNotNull(profile);
        assertEquals("", db.getTagFor(newSummoner));
    }

    @Test
    void testGetAllTags() throws SQLException, BeastSummonerDatabase.FailedToUpdateTagException {
        String tag1 = "what";
        String tag2 = "when";
        db.updateTagFor(summoner, tag1);
        db.updateTagFor(factory.createNewCreature(), tag2);

        List<String> allTags = db.getAllTags();
        assertEquals(2, allTags.size());
        assertTrue(allTags.contains(tag1));
        assertTrue(allTags.contains(tag2));
    }

    @Test
    void testGetAllTagsNoDuplicates() throws SQLException, BeastSummonerDatabase.FailedToUpdateTagException {
        String tag1 = "where";
        db.updateTagFor(summoner, tag1);
        db.updateTagFor(factory.createNewCreature(), tag1);

        List<String> allTags = db.getAllTags();
        assertEquals(1, allTags.size());
        assertTrue(allTags.contains(tag1));
    }

    @Test
    void testGetAllTagsChangedTags() throws SQLException, BeastSummonerDatabase.FailedToUpdateTagException {
        String tag1 = "why";
        String tag2 = "how";
        db.updateTagFor(summoner, tag1);
        db.updateTagFor(factory.createNewCreature(), tag2);
        db.updateTagFor(summoner, tag2);

        List<String> allTags = db.getAllTags();
        assertEquals(1, allTags.size());
        assertTrue(allTags.contains(tag2));
    }

    @Test
    void testGetProfileFor() {
        SummonerProfile profile1 = db.getProfileFor(summoner);
        SummonerProfile profile2 = db.getProfileFor(summoner);
        SummonerProfile profile3 = db.getProfileFor(factory.createNewCreature());

        assertEquals(profile1, profile2);
        assertNull(profile3);
    }

    @Test
    void testGetOptionsFor() throws NoSuchCreatureTemplateException {
        int n = 17;
        CreatureTemplate template1 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.CAT_WILD_CID);
        CreatureTemplate template2 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.CAVE_BUG_CID);
        createOptionUnique(template1, n);
        createOptionUnique(template2, n + 3);
        db.loadData();
        List<SummonOption> options1 = db.getOptionsFor(summoner);
        List<SummonOption> options2 = db.getOptionsFor(factory.createNewCreature());

        assertNotNull(options1);
        assertEquals(2, options1.size());
        SummonOption option1 = options1.get(0);
        assertEquals(template1, option1.template);
        assertEquals(n, option1.cap);
        assertEquals(n + 1, option1.price);
        assertEquals(CreatureTypeList.all.size(), option1.allowedTypes.size());
        SummonOption option2 = options1.get(1);
        assertEquals(template2, option2.template);
        assertEquals(n + 3, option2.cap);
        assertEquals(n + 4, option2.price);
        assertEquals(CreatureTypeList.all.size(), option2.allowedTypes.size());

        assertNull(options2);
    }

    @Test
    void testGetOptionsForTag() throws NoSuchCreatureTemplateException, BeastSummonerDatabase.FailedToUpdateTagException {
        int n = 17;
        String tag = "lies";
        CreatureTemplate template1 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.CAT_WILD_CID);
        CreatureTemplate template2 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.CAVE_BUG_CID);
        createOptionUnique(template1, n);
        createOptionTag(tag, template2, n + 5);
        db.loadData();
        db.updateTagFor(summoner, tag);
        Creature other = factory.createNewCreature();
        db.updateTagFor(other, tag);
        List<SummonOption> options1 = db.getOptionsFor(summoner);
        List<SummonOption> options2 = db.getOptionsFor(other);

        assertNotNull(options1);
        assertEquals(1, options1.size());
        SummonOption option = options1.get(0);
        assertEquals(template2, option.template);
        assertEquals(n + 5, option.cap);
        assertEquals(n + 6, option.price);
        assertEquals(CreatureTypeList.all.size(), option.allowedTypes.size());

        assertNotNull(options2);
        assertEquals(1, options2.size());
        SummonOption option2 = options2.get(0);
        assertEquals(template2, option2.template);
        assertEquals(n + 5, option2.cap);
        assertEquals(n + 6, option2.price);
        assertEquals(CreatureTypeList.all.size(), option2.allowedTypes.size());
    }

    @Test
    void testAddOptionUnique() throws NoSuchCreatureTemplateException, SQLException {
        int n = 28;
        CreatureTemplate template = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.CAT_WILD_CID);
        db.addOption(summoner, template, n, n + 1, Collections.emptySet());
        List<SummonOption> options = db.getOptionsFor(summoner);

        assertNotNull(options);
        assertEquals(1, options.size());
        SummonOption option = options.get(0);
        assertEquals(template, option.template);
        assertEquals(n + 1, option.cap);
        assertEquals(n, option.price);
        assertEquals(0, option.allowedTypes.size());
    }

    @Test
    void testAddOptionTag() throws NoSuchCreatureTemplateException, SQLException, BeastSummonerDatabase.FailedToUpdateTagException {
        int n = 32;
        CreatureTemplate template = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.CAT_WILD_CID);
        String tag = "something";
        db.updateTagFor(summoner, tag);
        db.addOption(tag, template, n, n + 1, Collections.emptySet());
        List<SummonOption> options = db.getOptionsFor(summoner);

        assertNotNull(options);
        assertEquals(1, options.size());
        SummonOption option = options.get(0);
        assertEquals(template, option.template);
        assertEquals(n + 1, option.cap);
        assertEquals(n, option.price);
        assertEquals(0, option.allowedTypes.size());
    }

    @Test
    void testUpdateOptionUnique() throws NoSuchCreatureTemplateException, SQLException {
        int n = 36;
        CreatureTemplate template1 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.ANACONDA_CID);
        CreatureTemplate template2 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.COW_BROWN_CID);
        db.addOption(summoner, template1, n, n + 1, Collections.emptySet());
        SummonOption oldOption = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);
        db.updateOption(summoner, oldOption, template2, n + 2, n + 3, new HashSet<>(CreatureTypeList.all));
        List<SummonOption> options = db.getOptionsFor(summoner);

        assertNotNull(options);
        assertEquals(1, options.size());
        SummonOption option = options.get(0);
        assertNotEquals(oldOption, option);
        assertEquals(template2, option.template);
        assertEquals(n + 3, option.cap);
        assertEquals(n + 2, option.price);
        assertEquals(CreatureTypeList.all.size(), option.allowedTypes.size());
    }

    @Test
    void testUpdateOptionTag() throws NoSuchCreatureTemplateException, SQLException, BeastSummonerDatabase.FailedToUpdateTagException {
        int n = 37;
        CreatureTemplate template1 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.BISON_CID);
        CreatureTemplate template2 = CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.BULL_CID);
        String tag = "thing";
        db.updateTagFor(summoner, tag);
        db.addOption(tag, template1, n, n + 1, Collections.emptySet());
        SummonOption oldOption = Objects.requireNonNull(db.getOptionsFor(summoner)).get(0);
        db.updateOption(summoner, oldOption, template2, n + 2, n + 3, new HashSet<>(CreatureTypeList.all));
        List<SummonOption> options = db.getOptionsFor(summoner);

        assertNotNull(options);
        assertEquals(1, options.size());
        SummonOption option = options.get(0);
        assertNotEquals(oldOption, option);
        assertEquals(template2, option.template);
        assertEquals(n + 3, option.cap);
        assertEquals(n + 2, option.price);
        assertEquals(CreatureTypeList.all.size(), option.allowedTypes.size());
    }

    @Test
    void testGetTagFor() throws BeastSummonerDatabase.FailedToUpdateTagException {
        String tag = "new";
        db.updateTagFor(summoner, tag);
        String actualTag = db.getTagFor(summoner);

        assertEquals(tag, actualTag);
    }

    @Test
    void testGetTagForEmptyStringOnNone() {
        String actualTag = db.getTagFor(summoner);

        assertTrue(actualTag.isEmpty());
    }

    @Test
    void testUpdateTag() throws BeastSummonerDatabase.FailedToUpdateTagException {
        String oldTag = "old";
        String newTag = "new";
        clearDb();
        createProfile(1, 1, oldTag);
        db.loadData();
        assert db.getTagFor(summoner).equals(oldTag);

        db.updateTagFor(summoner, newTag);
        String actualTag = db.getTagFor(summoner);

        assertNotEquals(oldTag, actualTag);
        assertEquals(newTag, actualTag);
    }

    @Test
    void testUpdateTagEmptyToNot() throws BeastSummonerDatabase.FailedToUpdateTagException {
        String oldTag = "";
        String newTag = "new";
        clearDb();
        createProfile(1, 1, oldTag);
        db.loadData();
        assert db.getTagFor(summoner).equals(oldTag);

        db.updateTagFor(summoner, newTag);
        String actualTag = db.getTagFor(summoner);

        assertNotEquals(oldTag, actualTag);
        assertEquals(newTag, actualTag);
    }

    @Test
    void testUpdateTagNotToEmpty() throws BeastSummonerDatabase.FailedToUpdateTagException {
        String oldTag = "old";
        String newTag = "";
        clearDb();
        createProfile(1, 1, oldTag);
        db.loadData();
        assert db.getTagFor(summoner).equals(oldTag);

        db.updateTagFor(summoner, newTag);
        String actualTag = db.getTagFor(summoner);

        assertNotEquals(oldTag, actualTag);
        assertEquals(newTag, actualTag);
    }

    @Test
    void testSetSpawnFor() throws SQLException {
        VolaTile tile = Zones.getOrCreateTile(1000, 1000, true);
        int n = 47;
        SummonerProfile oldProfile = Objects.requireNonNull(db.getProfileFor(summoner));
        assert oldProfile.spawnPointCentre != tile;
        assert oldProfile.acceptsCoin;

        db.setSpawnFor(summoner, tile, n, n + 1);
        SummonerProfile profile = db.getProfileFor(summoner);

        assertEquals(tile, Objects.requireNonNull(profile).spawnPointCentre);
        assertEquals(n, profile.range);
        assertEquals(n + 1, profile.floorLevel);
        assertTrue(profile.acceptsCoin);
    }

    @Test
    void testSetCurrencyFor() throws SQLException, NoSuchTemplateException {
        int currency = ItemList.ash;
        SummonerProfile oldProfile = Objects.requireNonNull(db.getProfileFor(summoner));
        assert oldProfile.acceptsCoin;

        db.setCurrencyFor(summoner, currency);
        SummonerProfile profile = db.getProfileFor(summoner);

        assertEquals(oldProfile.spawnPointCentre, Objects.requireNonNull(profile).spawnPointCentre);
        assertFalse(profile.acceptsCoin);
        assertEquals(currency, Objects.requireNonNull(profile.currency).getTemplateId());
    }

    @Test
    void testSetCurrencyForCoin() throws SQLException, NoSuchTemplateException {
        int currency = -1;
        int range = 12;
        clearDb();
        createProfile(range, ItemList.diamond, "");
        db.loadData();
        SummonerProfile oldProfile = Objects.requireNonNull(db.getProfileFor(summoner));
        assert !oldProfile.acceptsCoin;

        db.setCurrencyFor(summoner, currency);
        SummonerProfile profile = db.getProfileFor(summoner);

        assertEquals(oldProfile.spawnPointCentre, Objects.requireNonNull(profile).spawnPointCentre);
        assertTrue(profile.acceptsCoin);
        assertNull(profile.currency);
        assertEquals(range, profile.range);
    }

    @Test
    void testSetCurrencyForCoinToCoin() throws SQLException, NoSuchTemplateException {
        SummonerProfile oldProfile = Objects.requireNonNull(db.getProfileFor(summoner));
        assert oldProfile.acceptsCoin;

        db.setCurrencyFor(summoner, -1);
        SummonerProfile profile = db.getProfileFor(summoner);

        assertEquals(oldProfile.spawnPointCentre, Objects.requireNonNull(profile).spawnPointCentre);
        assertTrue(profile.acceptsCoin);
        assertNull(profile.currency);
    }

    @Test
    void testDeleteSummoner() throws SQLException {
        db.deleteSummoner(summoner);
        SummonerProfile profile = db.getProfileFor(summoner);

        assertNull(profile);
        db.loadData();
        assertNull(db.getProfileFor(summoner));
    }

    @Test
    void testDeleteAllOptionsFor() throws SQLException, NoSuchCreatureTemplateException {
        db.deleteAllOptionsFor(summoner);
        createOptionUnique(CreatureTemplateFactory.getInstance().getTemplate(CreatureTemplateIds.DOLPHIN_CID), 5);
        db.loadData();
        assert !Objects.requireNonNull(db.getOptionsFor(summoner)).isEmpty();

        db.deleteAllOptionsFor(summoner);

        SummonerProfile profile = db.getProfileFor(summoner);
        assertNotNull(profile);
        assertNull(db.getOptionsFor(summoner));
        db.loadData();
        assertNull(db.getOptionsFor(summoner));
    }
}
