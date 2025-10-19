package me.profelements.dynatech.utils;

import org.bukkit.inventory.ItemStack;

public class Utils {
    private Utils() {}

    public static ItemStack withAmount(ItemStack base, int amount) {
        if (base == null) return null;
        ItemStack copy = base.clone();
        copy.setAmount(amount);
        return copy;
    }
}
