package com.wurmonline.server.questions;

import mod.wurmunlimited.npcs.beastsummoner.SummonOption;

class SummonRequestDetails {
    final SummonOption option;
    final int age;
    final int amount;
    final int price;

    SummonRequestDetails(SummonOption option, int age, int amount) {
        this.option = option;
        this.age = age;
        this.amount = amount;
        this.price = option.price * amount;
    }
}
