package com.wurmonline.server.economy;

import com.wurmonline.server.DbConnector;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationTarget;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class BeastSummonerEconomy {
    private static final Logger logger = Logger.getLogger(BeastSummonerEconomy.class.getName());

    public static Shop createShop(long wurmId) {
        return new DbShop(wurmId, 0);
    }

    public static Shop findOrCreateShopFor(long wurmId) {
        try {
            Shop getShop = Economy.getEconomy().getShop(Creatures.getInstance().getCreature(wurmId));

            for (Shop shop : Economy.getEconomy().getShops()) {
                if (shop.wurmid == wurmId) {
                    if (getShop == null) {
                        try {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Shop exists in getShops(), but cannot get via getShop().");
                            Map<String, InvocationTarget> targets = ReflectionUtil.getPrivateField(HookManager.getInstance(), HookManager.class.getDeclaredField("invocationTargets"));
                            for (String target : targets.keySet()) {
                                if (target.startsWith("com.wurmonline.server.economy.Economy.getShop")) {
                                    InvocationTarget t = targets.get(target);
                                    if (t == null) {
                                        sb.append("\nnull");
                                    } else {
                                        InvocationHandlerFactory factory = ReflectionUtil.getPrivateField(t, InvocationTarget.class.getDeclaredField("invocationHandlerFactory"));
                                        String name = factory.getClass().getName();

                                        if (!name.startsWith("mod.wurmunlimited.npcs")) {
                                            sb.append("\n");
                                            sb.append(name);
                                        }
                                    }
                                }
                            }

                            logger.warning(sb.toString());
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            logger.warning("Shop exists in getShops(), but cannot get via getShop().  Invocation field error:");
                            e.printStackTrace();
                        }

                        return shop;
                    } else {
                        logger.info("Shop exists in getShops(), and via getShop().");
                        return getShop;
                    }
                }
            }

            logger.warning("Shop for summoner (" + wurmId + ") did not exist, creating a new one.");
            Shop newlyCreated = createShop(wurmId);

            Shop maybeNewShop = Economy.getEconomy().getShop(Creatures.getInstance().getCreature(wurmId));
            if (maybeNewShop != null && Objects.equals(maybeNewShop, newlyCreated)) {
                logger.info("Got newly created shop via getShop().");
                return newlyCreated;
            }

            logger.info("Could not get newly created shop via getShop() (it returned " + maybeNewShop + ").");

            for (Shop shop : Economy.getEconomy().getShops()) {
                if (shop.wurmid == newlyCreated.wurmid) {
                    Shop alsoMaybeNewShop = Economy.getEconomy().getShop(Creatures.getInstance().getCreature(wurmId));
                    if (alsoMaybeNewShop == null) {
                        logger.warning("Newly created exists in getShops(), but cannot get via getShop().");
                    } else {
                        logger.info("Newly created exists in getShops(), and via getShop().");
                    }
                    return newlyCreated;
                }
            }

            logger.info("Newly created does not exist in getShops().");

            try {
                @SuppressWarnings("SqlResolve")
                ResultSet rs = DbConnector.getEconomyDbCon().prepareStatement("SELECT * FROM TRADER WHERE WURMID=?;").executeQuery();

                while (rs.next()) {
                    logger.info(wurmId + " found in database.");
                }
            } catch (SQLException e) {
                logger.warning("Database exception thrown:");
                e.printStackTrace();
            }

            return newlyCreated;
        } catch (NoSuchCreatureException e) {
            logger.warning("Creature (" + wurmId + ") did not exist.");
            e.printStackTrace();
        }

        return createShop(wurmId);
    }
}
