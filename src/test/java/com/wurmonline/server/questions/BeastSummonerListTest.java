package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.CreatureTemplateFactory;
import com.wurmonline.server.creatures.NoSuchCreatureTemplateException;
import mod.wurmunlimited.npcs.beastsummoner.BeastSummonerTest;

import java.util.Arrays;
import java.util.Random;

public class BeastSummonerListTest extends BeastSummonerTest {
    // Should probably just stop using random, but I think the fuzziness helps a bit.
    private static int lastTemplateId = -1;

    protected CreatureTemplate getRandomTemplate() {
        try {
            int templateId = new Random().nextInt(119);
            while (templateId == lastTemplateId) {
                templateId = new Random().nextInt(119);
            }
            // These templates do not exist.
            if (Arrays.asList(
                    0, 4, 5, 6, 7, 24, 114, 115, 116
            ).contains(templateId)) {
                if (lastTemplateId == 1) {
                    templateId = 2;
                } else {
                    templateId = 1;
                }
            }
            lastTemplateId = templateId;
            return CreatureTemplateFactory.getInstance().getTemplate(templateId);
        } catch (NoSuchCreatureTemplateException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getRegex(String template) {
        return "([\",]" + template + "[\",])";
    }
}
