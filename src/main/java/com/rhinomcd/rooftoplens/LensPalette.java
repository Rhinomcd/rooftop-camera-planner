package com.rhinomcd.rooftoplens;

import java.awt.Color;

final class LensPalette
{
    static final Color TEAL = new Color(63, 208, 201);
    static final Color AMBER = new Color(242, 184, 75);
    static final Color CORAL = new Color(255, 107, 107);
    static final Color INK = new Color(12, 17, 23, 228);
    static final Color RULE = new Color(255, 255, 255, 28);
    static final Color TEXT = new Color(232, 238, 242);
    static final Color MUTED = new Color(140, 156, 168);

    private LensPalette()
    {
    }

    static Color alpha(Color color, int alpha)
    {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
