package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.beastsummoner.SummonOption;
import mod.wurmunlimited.npcs.beastsummoner.SummonerProfile;

import java.util.List;

public class SummonRequest {
    public static class SummonRequestDetails {
        final SummonOption option;
        final byte type;
        final byte age;
        final int amount;
        public final int price;
        public final String name;

        SummonRequestDetails(SummonOption option, byte type, byte age, int amount) {
            this.option = option;
            this.type = type;
            this.age = age;
            this.amount = amount;
            this.price = option.price * amount;
            name = getTypeString(type) + age(age) + " " + option.template.getName() + " x " + amount;
        }

        private String age(byte age) {
            if (age < 3) {
                return "young";
            }
            if (age < 8) {
                return "adolescent";
            }
            if (age < 12) {
                return "mature";
            }
            if (age < 30) {
                return "aged";
            }
            if (age < 40) {
                return "old";
            }
            return "venerable";
        }

        private final String getTypeString(byte type) {
            if (type <= 0) {
                return "";
            }
            switch (type) {
                case 1: {
                    return "fierce ";
                }
                case 2: {
                    return "angry ";
                }
                case 3: {
                    return "raging ";
                }
                case 4: {
                    return "slow ";
                }
                case 5: {
                    return "alert ";
                }
                case 6: {
                    return "greenish ";
                }
                case 7: {
                    return "lurking ";
                }
                case 8: {
                    return "sly ";
                }
                case 9: {
                    return "hardened ";
                }
                case 10: {
                    return "scared ";
                }
                case 11: {
                    return "diseased ";
                }
                case 99: {
                    return "champion ";
                }
                default: {
                    return "";
                }
            }
        }
    }
    private final Creature summoner;
    private final Player requester;
    private final SummonerProfile profile;
    private final List<SummonRequestDetails> summons;
    
    SummonRequest(Creature summoner, Player requester, SummonerProfile profile, List<SummonRequestDetails> summons) {
        this.summoner = summoner;
        this.requester = requester;
        this.profile = profile;
        this.summons = summons;
    }

    public void doSummon() {
        for (SummonRequestDetails summon : summons) {
            summon.option.summon(summoner, requester, profile, summon.type, summon.amount, summon.age);
        }
    }

    // For testing.
    public int totalPrice() {
        return summons.stream().mapToInt(it -> it.price).sum();
    }
}
