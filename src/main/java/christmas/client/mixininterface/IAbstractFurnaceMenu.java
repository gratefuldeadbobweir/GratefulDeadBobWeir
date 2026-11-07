/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixininterface;

import net.minecraft.world.item.ItemStack;

public interface IAbstractFurnaceMenu {
    boolean christmas$canSmelt(ItemStack itemStack);
}
