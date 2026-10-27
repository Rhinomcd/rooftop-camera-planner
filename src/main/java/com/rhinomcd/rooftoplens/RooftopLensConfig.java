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
        description = "In a view you have lapped before, mark where you usually click each obstacle")
    default boolean showRoute()
    {
        return true;
    }

    @ConfigItem(keyName = "showAll", name = "Outline every obstacle", position = 2,
        description = "Outline all visible obstacles on the course, not only the next one")
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
    @ConfigItem(keyName = "routeColor", name = "Other obstacles", position = 4,
        description = "Color of other obstacles and their click spots")
    default Color routeColor()
    {
        return LensPalette.TEAL;
    }
}
