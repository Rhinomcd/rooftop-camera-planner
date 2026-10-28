package com.rhinomcd.rooftoplens;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RooftopLensConfig.GROUP)
public interface RooftopLensConfig extends Config
{
    String GROUP = "rooftopLens";

    @ConfigItem(keyName = "showRoute", name = "Show click spots", position = 1,
        description = "In a view you have lapped before, mark where each obstacle will be when you usually click it, so the mouse can wait there")
    default boolean showRoute()
    {
        return true;
    }

    @ConfigItem(keyName = "showAll", name = "Outline every obstacle", position = 2,
        description = "Outline all visible obstacles and show every click spot, not only the next two")
    default boolean showAll()
    {
        return true;
    }

    @Alpha
    @ConfigItem(keyName = "nextColor", name = "Next obstacle", position = 3,
        description = "Color of the obstacle to click next")
    default Color nextColor()
    {
        return LensPalette.AMBER;
    }

    @Alpha
    @ConfigItem(keyName = "readyColor", name = "Click now", position = 4,
        description = "Color of the next click spot once the previous obstacle is done and the click will count")
    default Color readyColor()
    {
        return LensPalette.GO;
    }

    @Alpha
    @ConfigItem(keyName = "routeColor", name = "Other obstacles", position = 5,
        description = "Color of other obstacles and their click spots")
    default Color routeColor()
    {
        return LensPalette.TEAL;
    }
}
