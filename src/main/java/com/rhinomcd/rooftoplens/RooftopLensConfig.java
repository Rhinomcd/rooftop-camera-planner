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

    @ConfigItem(keyName = "showRoute", name = "Show learned route", position = 1,
        description = "In a view you have lapped before, draw your usual click spots joined in lap order")
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
    @ConfigItem(keyName = "routeColor", name = "Route", position = 4,
        description = "Color of other obstacles and the learned route")
    default Color routeColor()
    {
        return LensPalette.TEAL;
    }
}
