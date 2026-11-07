/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.settings;

import christmas.client.gui.GuiTheme;
import christmas.client.gui.WidgetScreen;
import christmas.client.utils.misc.IChangeable;
import christmas.client.utils.misc.ICopyable;
import christmas.client.utils.misc.ISerializable;
import net.minecraft.world.level.block.Block;

public interface IBlockData<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> {
    WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<T> setting);
}
