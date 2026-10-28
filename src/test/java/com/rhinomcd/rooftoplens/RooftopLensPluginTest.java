package com.rhinomcd.rooftoplens;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RooftopLensPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(RooftopLensPlugin.class);
        RuneLite.main(args);
    }
}
