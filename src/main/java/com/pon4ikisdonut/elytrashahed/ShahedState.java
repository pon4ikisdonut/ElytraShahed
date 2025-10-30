package com.pon4ikisdonut.elytrashahed;

import org.bukkit.inventory.ItemStack;

class ShahedState {
    private final ItemStack previousHelmet;
    private int tntScale;

    ShahedState(ItemStack previousHelmet) {
        this(previousHelmet, 1);
    }

    ShahedState(ItemStack previousHelmet, int tntScale) {
        this.previousHelmet = previousHelmet;
        this.tntScale = Math.max(1, tntScale);
    }

    ItemStack previousHelmet() {
        return previousHelmet;
    }

    int getTntScale() {
        return tntScale;
    }

    void setTntScale(int tntScale) {
        this.tntScale = Math.max(1, tntScale);
    }
}
