package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerMod;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SetSpawnQuestion extends BeastSummonerQuestionExtension {
    private final Creature settingFor;
    private final VolaTile tile;
    private final int floorLevel;
    private int range;

    public SetSpawnQuestion(Creature performer, Creature settingFor, VolaTile tile, int floorLevel) {
        super(performer, "Set Spawn", "", MANAGETRADER, settingFor.getWurmId());
        this.settingFor = settingFor;
        this.tile = tile;
        this.floorLevel = floorLevel;
        SummonerProfile profile = BeastSummonerMod.mod.db.getProfileFor(settingFor);
        if (profile != null) {
            this.range = profile.range;
        } else {
            this.range = 0;
        }
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);
        Creature responder = getResponder();
        removeMarkers();

        if (wasSelected("cancel")) {
            return;
        }

        try {
            String val = answers.getProperty("range");
            int newRange = range;
            if (val != null && !val.isEmpty()) {
                newRange = Integer.parseInt(val);
            }
            if (newRange < 0) {
                responder.getCommunicator().sendNormalServerMessage("Range must be 0 or greater, setting " + range + ".");
            } else {
                range = newRange;
            }
        } catch (NumberFormatException e) {
            responder.getCommunicator().sendNormalServerMessage("Invalid range supplied, setting " + range + ".");
        }

        if (wasSelected("survey")) {
            for (int[] xY : perimeterTiles()) {
                int x = xY[0];
                int y = xY[1];

                try {
                    final Item item = ItemFactory.createItem(ItemList.buildMarker, 80.0f, this.getResponder().getName());
                    item.setPosXYZ((float)((x << 2) + 2), (float)((y << 2) + 2), Zones.calculateHeight((float)((x << 2) + 2), (float)((y << 2) + 2), tile.isOnSurface()) + 5.0f);
                    // TODO - What about floor?
                    Zones.getZone(x, y, tile.isOnSurface()).addItem(item);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            sendQuestion();
        } else if (wasSelected("submit")) {
            try {
                BeastSummonerMod.mod.db.setSpawnFor(settingFor, tile, range, floorLevel);
            } catch (SQLException e) {
                responder.getCommunicator().sendAlertServerMessage("Something went wrong and the spawn was not set.");
                e.printStackTrace();
            }
        }
    }

    private List<int[]> perimeterTiles() {
        List<int[]> tiles = new ArrayList<>();
        final int centreX = tile.getTileX();
        final int centreY = tile.getTileY();
        final int xa = Zones.safeTileX(centreX - range);
        final int xe = Zones.safeTileX(centreX + range);
        final int ya = Zones.safeTileY(centreY - range);
        final int ye = Zones.safeTileY(centreY + range);
        boolean notFound = false;
        for (int x = xa; x <= xe; ++x) {
            for (int y = ya; y <= ye; ++y) {
                boolean toAdd = false;
                if (x == xa) {
                    if (y == ya || y == ye || y % 10 == 0) {
                        toAdd = true;
                    }
                } else if (x == xe) {
                    if (y == ya || y == ye || y % 10 == 0) {
                        toAdd = true;
                    }
                } else if ((y == ya || y == ye) && x % 10 == 0) {
                    toAdd = true;
                }

                if (toAdd) {
                    tiles.add(new int[] { x, y });
                }
            }
        }

        return tiles;
    }

    private void removeMarkers() {
        for (int[] xY : perimeterTiles()) {
            int x = xY[0];
            int y = xY[1];

            try {
                final Zone zone = Zones.getZone(x, y, tile.isOnSurface());
                final VolaTile tile = zone.getTileOrNull(x, y);
                if (tile != null) {
                    boolean notFound = true;
                    for (Item item : tile.getItems()) {
                        if (item.getTemplateId() == ItemList.buildMarker) {
                            notFound = false;
                            item.removeAndEmpty();
                            break;
                        }
                    }

                    if (notFound) {
                        return;
                    }
                }
            } catch (NoSuchZoneException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    @Override
    public void sendQuestion() {
        String bml = new BMLBuilder(id)
                             .text("You are about to set the spawn for " + settingFor.getName() + ", at " +
                                           tile.getTileX() + ", " + tile.getTileY() + ".")
                             .harray(b -> b.label("Range: ").entry("range", Integer.toString(range), 3))
                             .text("How far in tiles the summoner will spawn beasts from this point.")
                             .harray(b -> b.button("submit", "Send").spacer().button("survey", "Survey Area").spacer().button("cancel", "Cancel"))
                             .build();

        getResponder().getCommunicator().sendBml(250, 300, true, true, bml, 200, 200, 200, title);
    }
}
