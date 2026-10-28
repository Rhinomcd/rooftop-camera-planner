package com.rhinomcd.rooftoplens;

import java.awt.Color;

final class LensPalette
{
    static final Color TEAL = new Color(63, 208, 201);
    static final Color AMBER = new Color(242, 184, 75);
    static final Color GO = Color.GREEN;
    static final Color INK = new Color(12, 17, 23, 228);
    static final Color TEXT = new Color(232, 238, 242);

    private LensPalette()
    {
    }

    static Color alpha(Color color, int alpha)
    {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
