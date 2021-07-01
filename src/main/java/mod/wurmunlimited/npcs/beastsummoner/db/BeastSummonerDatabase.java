package mod.wurmunlimited.npcs.beastsummoner.db;

import com.wurmonline.server.creatures.*;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.questions.CreatureTypeList;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.npcs.beastsummoner.SummonOption;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;
import mod.wurmunlimited.npcs.db.Database;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BeastSummonerDatabase extends Database {
    public static class FailedToUpdateTagException extends Exception {
        FailedToUpdateTagException(SQLException e) {
            super(e);
        }
    }

    private static final Logger logger = Logger.getLogger(BeastSummonerDatabase.class.getName());
    public String tagDumpDbString = "mods/beastsummoner/tags.db";
    private final Map<Creature, SummonerProfile> allProfiles = new HashMap<>();
    private final Map<Creature, List<SummonOption>> allOptions = new HashMap<>();
    private final Map<Creature, String> allTags = new HashMap<>();
    private final Map<String, List<SummonOption>> allTagOptions = new HashMap<>();

    public BeastSummonerDatabase(String dbName) {
        super(dbName);
    }

    @Override
    protected void doInit(Connection db) throws SQLException {
        db.prepareStatement("CREATE TABLE IF NOT EXISTS summoner (" +
                                    "id INTEGER NOT NULL UNIQUE," +
                                    "x INTEGER NOT NULL," +
                                    "y INTEGER NOT NULL," +
                                    "surfaced INTEGER NOT NULL," +
                                    "range INTEGER NOT NULL," +
                                    "floor_level INTEGER NOT NULL," +
                                    "currency INTEGER NOT NULL," +
                                    "tag TEXT NOT NULL" +
                                    ");").execute();

        db.prepareStatement("CREATE TABLE IF NOT EXISTS options (" +
                                    "id INTEGER NOT NULL," +
                                    "template INTEGER NOT NULL," +
                                    "cap INTEGER NOT NULL," +
                                    "price INTEGER NOT NULL," +
                                    "types TEXT NOT NULL," +
                                    "UNIQUE(id, template));").execute();

        db.prepareStatement("CREATE TABLE IF NOT EXISTS tag_options (" +
                                    "tag TEXT NOT NULL," +
                                    "template INTEGER NOT NULL," +
                                    "cap INTEGER NOT NULL," +
                                    "price INTEGER NOT NULL," +
                                    "types TEXT NOT NULL," +
                                    "UNIQUE(tag, template));").execute();
    }

    @SuppressWarnings({"SqlResolve", "SqlInsertValues"})
    public void loadTags() throws SQLException {
        File file = new File(tagDumpDbString);
        if (file.exists()) {
            execute(db -> {
                db.prepareStatement("ATTACH DATABASE '" + tagDumpDbString + "' AS dump;").execute();
                db.prepareStatement("INSERT OR IGNORE INTO main.tag_options SELECT * FROM dump.tag_options;").execute();
                db.prepareStatement("DETACH dump;").execute();
            });

            loadTagData();
        }
    }

    @SuppressWarnings("SqlResolve")
    public void dumpTags() throws SQLException {
        Connection db2 = DriverManager.getConnection("jdbc:sqlite:" + tagDumpDbString);
        db2.close();
        execute(db -> {
            db.prepareStatement("ATTACH DATABASE '" + tagDumpDbString + "' AS dump;").execute();
            db.prepareStatement("DROP TABLE IF EXISTS dump.tag_options;").execute();
            db.prepareStatement("CREATE TABLE dump.tag_options AS SELECT * FROM main.tag_options;").execute();
            db.prepareStatement("DETACH dump;").execute();
        });
    }

    private String typesString(Set<Byte> types) {
        return types.size() == CreatureTypeList.all.size() ? "all" : types.stream().map(Object::toString).collect(Collectors.joining());
    }

    private Set<Byte> parseTypes(String types) {
        Set<Byte> allowedTypes = new HashSet<>();

        if (types.equals("all")) {
            allowedTypes.addAll(CreatureTypeList.all);
        } else {
            String[] parts = types.split(",");
            for (String part : parts) {
                try {
                    allowedTypes.add(Byte.parseByte(part));
                } catch (NumberFormatException e) {
                    logger.warning("Unknown creature type byte encountered - " + part);
                }
            }
        }

        return allowedTypes;
    }

    @SuppressWarnings("DuplicatedCode")
    public void loadData() {
        allProfiles.clear();
        allOptions.clear();

        try {
            execute(db -> {
                ResultSet profiles = db.prepareStatement("SELECT * FROM summoner;").executeQuery();
                while (profiles.next()) {
                    long id = profiles.getLong(1);
                    Creature creature = Creatures.getInstance().getCreatureOrNull(id);
                    if (creature != null) {
                        int x = profiles.getInt(2);
                        int y = profiles.getInt(3);
                        boolean surfaced = profiles.getBoolean(4);
                        VolaTile spawn = Zones.getOrCreateTile(x, y, surfaced);

                        if (spawn == null) {
                            logger.warning("Spawn centre tile not found (" + x + ", " + y + ", " + surfaced + "), ignoring summoner.");
                            continue;
                        }

                        int range = profiles.getInt(5);
                        int floorLevel = profiles.getInt(6);
                        int currency = profiles.getInt(7);
                        String tag = profiles.getString(8);

                        SummonerProfile profile;
                        if (currency == -1) {
                            profile = new SummonerProfile(spawn, floorLevel, range);
                        } else {
                            try {
                                ItemTemplate currencyTemplate = ItemTemplateFactory.getInstance().getTemplate(currency);
                                profile = new SummonerProfile(spawn, floorLevel, range, currencyTemplate);
                            } catch (NoSuchTemplateException e) {
                                logger.warning("Currency template not found (" + currency + "), ignoring summoner.");
                                e.printStackTrace();
                                continue;
                            }
                        }
                        allProfiles.put(creature, profile);

                        if (!tag.isEmpty()) {
                            allTags.put(creature, tag);
                        }
                    } else {
                        logger.warning("Unknown creature (" + id + ") in database (summoner), ignoring.");
                    }
                }

                ResultSet options = db.prepareStatement("SELECT * FROM options;").executeQuery();
                while (options.next()) {
                    long id = options.getLong(1);
                    int templateId = options.getInt(2);
                    int cap = options.getInt(3);
                    int price = options.getInt(4);
                    String types = options.getString(5);
                    Creature creature = Creatures.getInstance().getCreatureOrNull(id);
                    if (creature == null) {
                        logger.warning("Unknown creature (" + id + ") in database (options), ignoring option.");
                        continue;
                    }
                    CreatureTemplate creatureTemplate;
                    try {
                        creatureTemplate = CreatureTemplateFactory.getInstance().getTemplate(templateId);
                    } catch (NoSuchCreatureTemplateException e) {
                        logger.warning("Creature template not found (" + templateId + "), ignoring option.");
                        e.printStackTrace();
                        continue;
                    }

                    List<SummonOption> optionsList = allOptions.computeIfAbsent(creature, k -> new ArrayList<>());
                    optionsList.add(new SummonOption(creatureTemplate, price, cap, parseTypes(types)));
                }

                loadTagData();
            });
        } catch (SQLException e) {
            logger.warning("Error occurred when loading summoner data, other data may not have been loaded.");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void loadTagData() throws SQLException {
        allTagOptions.clear();

        execute(db -> {
            ResultSet tagOptions = db.prepareStatement("SELECT * FROM tag_options;").executeQuery();
            while (tagOptions.next()) {
                String tag = tagOptions.getString(1);
                int templateId = tagOptions.getInt(2);
                int cap = tagOptions.getInt(3);
                int price = tagOptions.getInt(4);
                String types = tagOptions.getString(5);
                CreatureTemplate creatureTemplate;
                try {
                    creatureTemplate = CreatureTemplateFactory.getInstance().getTemplate(templateId);
                } catch (NoSuchCreatureTemplateException e) {
                    logger.warning("Creature template not found (" + templateId + "), ignoring option.");
                    e.printStackTrace();
                    continue;
                }

                List<SummonOption> optionsList = allTagOptions.computeIfAbsent(tag, k -> new ArrayList<>());
                optionsList.add(new SummonOption(creatureTemplate, price, cap, parseTypes(types)));
            }
        });
    }

    private void addNew(Creature creature, VolaTile spawn, int floorLevel, int range, int currency, String tag) throws SQLException {
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("INSERT OR IGNORE INTO summoner (id, x, y, surfaced, floor_level, range, currency, tag) VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
            ps.setLong(1, creature.getWurmId());
            ps.setInt(2, spawn.getTileX());
            ps.setInt(3, spawn.getTileY());
            ps.setBoolean(4, spawn.isOnSurface());
            ps.setInt(5, floorLevel);
            ps.setInt(6, range);
            ps.setInt(7, currency);
            ps.setString(8, tag);
            ps.execute();
        });

        if (!tag.isEmpty()) {
            allTags.put(creature, tag);
        }
    }

    public void addNew(Creature creature, VolaTile spawn, int floorLevel, int range, ItemTemplate currency, String tag) throws SQLException {
        addNew(creature, spawn, floorLevel, range, currency.getTemplateId(), tag);
        allProfiles.put(creature, new SummonerProfile(spawn, floorLevel, range, currency));
    }

    public void addNew(Creature creature, VolaTile spawn, int floorLevel, int range, String tag) throws SQLException {
        addNew(creature, spawn, floorLevel, range, -1, tag);
        allProfiles.put(creature, new SummonerProfile(spawn, floorLevel, range));
    }

    public List<String> getAllTags() {
        return allTags.values().stream().distinct().sorted().collect(Collectors.toList());
    }

    public @Nullable SummonerProfile getProfileFor(Creature summoner) {
        return allProfiles.get(summoner);
    }

    public @Nullable List<SummonOption> getOptionsFor(Creature summoner) {
        String tag = allTags.get(summoner);
        if (tag != null) {
            return allTagOptions.get(tag);
        }
        return allOptions.get(summoner);
    }

    public SummonOption addOption(Creature summoner, CreatureTemplate template, int price, int cap, Set<Byte> allowedTypes) throws SQLException {
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("INSERT INTO options (id, template, cap, price, types) VALUES(?, ?, ?, ?, ?);");
            ps.setLong(1, summoner.getWurmId());
            ps.setInt(2, template.getTemplateId());
            ps.setInt(3, cap);
            ps.setInt(4, price);
            ps.setString(5, typesString(allowedTypes));
            ps.execute();
        });

        List<SummonOption> options = allOptions.computeIfAbsent(summoner, k -> new ArrayList<>());
        SummonOption option = new SummonOption(template, price, cap, new HashSet<>(allowedTypes));
        options.add(option);
        return option;
    }

    public SummonOption addOption(String tag, CreatureTemplate template, int price, int cap, Set<Byte> allowedTypes) throws SQLException {
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("INSERT INTO tag_options (tag, template, cap, price, types) VALUES(?, ?, ?, ?, ?);");
            ps.setString(1, tag);
            ps.setInt(2, template.getTemplateId());
            ps.setInt(3, cap);
            ps.setInt(4, price);
            ps.setString(5, typesString(allowedTypes));
            ps.execute();
        });

        List<SummonOption> options = allTagOptions.computeIfAbsent(tag, k -> new ArrayList<>());
        SummonOption option = new SummonOption(template, price, cap, new HashSet<>(allowedTypes));
        options.add(option);
        return option;
    }


    public SummonOption updateOption(Creature summoner, @Nullable SummonOption oldOption, CreatureTemplate template, int price, int cap, Set<Byte> allowedTypes) throws SQLException {
        boolean updated = false;
        String tag = allTags.get(summoner);
        List<SummonOption> options;
        if (tag != null) {
            if (oldOption == null) {
                return addOption(tag, template, price, cap, allowedTypes);
            } else {
                options = allTagOptions.computeIfAbsent(tag, k -> new ArrayList<>());
                execute(db -> {
                    PreparedStatement ps = db.prepareStatement("UPDATE tag_options SET cap=?, price=?, types=? WHERE tag=? AND template=?;");
                    ps.setInt(1, cap);
                    ps.setInt(2, price);
                    ps.setString(3, typesString(allowedTypes));
                    ps.setString(4, tag);
                    ps.setInt(5, template.getTemplateId());
                    ps.execute();
                });
            }
        } else {
            if (oldOption == null) {
                return addOption(summoner, template, price, cap, allowedTypes);
            } else {
                options = allOptions.computeIfAbsent(summoner, k -> new ArrayList<>());
                execute(db -> {
                    PreparedStatement ps = db.prepareStatement("UPDATE options SET cap=?, price=?, types=? WHERE id=? AND template=?;");
                    ps.setInt(1, cap);
                    ps.setInt(2, price);
                    ps.setString(3, typesString(allowedTypes));
                    ps.setLong(4, summoner.getWurmId());
                    ps.setInt(5, template.getTemplateId());
                    ps.execute();
                });
            }
        }

        SummonOption newOption = new SummonOption(template, price, cap, allowedTypes);
        int index = options.indexOf(oldOption);
        if (index >= 0){
            options.set(index, newOption);
        } else {
            logger.warning("Could not find index of expected summon option.");
            options.add(newOption);
        }
        return newOption;
    }

    public String getTagFor(Creature summoner) {
        return allTags.getOrDefault(summoner, "");
    }

    public void updateTagFor(Creature summoner, String tag) throws FailedToUpdateTagException {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE summoner SET tag=? WHERE id=?;");
                ps.setString(1, tag);
                ps.setLong(2, summoner.getWurmId());
                ps.execute();
            });

            if (tag.isEmpty()) {
                allTags.remove(summoner);
            } else {
                allTags.put(summoner, tag);
            }
        } catch (SQLException e) {
            throw new FailedToUpdateTagException(e);
        }
    }

    public void renameTag(String oldTag, String newTag) throws FailedToUpdateTagException {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE summoner SET tag=? WHERE tag=?;");
                ps.setString(1, newTag);
                ps.setString(2, oldTag);
                ps.execute();
            });

            allTags.replaceAll((creature, s) -> s.equals(oldTag) ? newTag : s);
            List<SummonOption> options = allTagOptions.remove(oldTag);
            if (options != null) {
                allTagOptions.put(newTag, options);
            }
        } catch (SQLException e) {
            throw new FailedToUpdateTagException(e);
        }
    }

    public void deleteTag(String tag) throws FailedToUpdateTagException {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE summoner SET tag=? WHERE tag=?;");
                ps.setString(1, "");
                ps.setString(2, tag);
                ps.execute();
            });

            execute(db -> {
                PreparedStatement ps = db.prepareStatement("DELETE FROM tag_options WHERE tag=?;");
                ps.setString(1, tag);
                ps.execute();
            });

            allTags.values().removeIf(tag::equals);
            allTagOptions.remove(tag);
        } catch (SQLException e) {
            throw new FailedToUpdateTagException(e);
        }
    }

    public void setSpawnFor(Creature summoner, VolaTile tile, int range, int floorLevel) throws SQLException {
        SummonerProfile profile = allProfiles.remove(summoner);

        if (profile != null) {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE summoner SET x=?, y=?, surfaced=?, range=?, floor_level=? WHERE id=?;");
                ps.setInt(1, tile.getTileX());
                ps.setInt(2, tile.getTileY());
                ps.setBoolean(3, tile.isOnSurface());
                ps.setInt(4, range);
                ps.setInt(5, floorLevel);
                ps.setLong(6, summoner.getWurmId());
                ps.execute();
            });

            SummonerProfile newProfile;
            if (profile.acceptsCoin) {
                newProfile = new SummonerProfile(tile, floorLevel, range);
            } else {
                newProfile = new SummonerProfile(tile, floorLevel, range, profile.currency);
            }
            allProfiles.put(summoner, newProfile);
        } else {
            logger.warning("Attempted to set spawn for non-existent summoner profile " + summoner.getName() + " (" + summoner.getWurmId() + ").");
        }
    }

    public void setCurrencyFor(Creature summoner, int currency) throws SQLException, NoSuchTemplateException {
        ItemTemplate currencyTemplate = null;
        if (currency != -1) {
            currencyTemplate = ItemTemplateFactory.getInstance().getTemplate(currency);
        }

        execute(db -> {
            PreparedStatement ps = db.prepareStatement("UPDATE summoner SET currency=? WHERE id=?;");
            ps.setInt(1, currency);
            ps.setLong(2, summoner.getWurmId());
            ps.execute();
        });

        SummonerProfile oldProfile = allProfiles.get(summoner);
        SummonerProfile newProfile;
        if (currency == -1) {
            newProfile = new SummonerProfile(oldProfile.spawnPointCentre, oldProfile.floorLevel, oldProfile.range);
        } else {
            newProfile = new SummonerProfile(oldProfile.spawnPointCentre, oldProfile.floorLevel, oldProfile.range, currencyTemplate);
        }
        allProfiles.put(summoner, newProfile);
    }

    public void deleteSummoner(Creature summoner) throws SQLException {
        SummonerProfile removed = allProfiles.remove(summoner);

        if (removed != null) {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("DELETE FROM summoner WHERE id=?;");
                ps.setLong(1, summoner.getWurmId());
                ps.execute();
            });
        }

        deleteAllOptionsFor(summoner);
    }

    public void deleteOption(String tag, SummonOption option) {
        List<SummonOption> options = allTagOptions.get(tag);
        if (options != null && options.remove(option)) {
            try {
                execute(db -> {
                    PreparedStatement ps = db.prepareStatement("DELETE FROM tag_options WHERE tag=? AND template=?;");
                    ps.setString(1, tag);
                    ps.setInt(2, option.template.getTemplateId());
                    ps.execute();
                });
            } catch (SQLException e) {
                logger.warning("Error when deleting tag option for \"" + tag + "\".");
                e.printStackTrace();
            }
        }
    }

    public void deleteOption(Creature summoner, SummonOption option) {
        List<SummonOption> options = allOptions.get(summoner);
        if (options != null && options.remove(option)) {
            try {
                execute(db -> {
                    PreparedStatement ps = db.prepareStatement("DELETE FROM options WHERE id=? AND template=?;");
                    ps.setLong(1, summoner.getWurmId());
                    ps.setInt(2, option.template.getTemplateId());
                    ps.execute();
                });
            } catch (SQLException e) {
                logger.warning("Error when deleting option for " + summoner.getName() + " (" + summoner.getWurmId() + ").");
                e.printStackTrace();
            }
        }
    }

    public void deleteAllOptionsFor(Creature summoner) throws SQLException {
        List<SummonOption> removed = allOptions.remove(summoner);

        if (removed != null) {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("DELETE FROM options WHERE id=?;");
                ps.setLong(1, summoner.getWurmId());
                ps.execute();
            });
        }
    }
}
