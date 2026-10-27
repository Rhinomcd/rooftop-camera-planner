package com.rhinomcd.rooftoplens;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CameraPoseTest
{
    @Test
    public void yawWrapsAroundNorth()
    {
        assertEquals(32, new CameraPose(2032, 300).yawDelta(16));
        assertEquals(-32, new CameraPose(16, 300).yawDelta(2032));
        assertEquals("0:304", new CameraPose(2040, 300).viewKey());
    }

    @Test
    public void aPoseThatHoldsASnappedViewSharesItsKey()
    {
        CameraPose view = CameraPose.fromViewKey("512:384");
        for (int dy = -24; dy <= 24; dy += 8)
        {
            for (int dp = -7; dp <= 7; dp++)
            {
                CameraPose nearby = new CameraPose(512 + dy, 384 + dp);
                assertTrue(nearby.holds(view));
                assertEquals(view.viewKey(), nearby.viewKey());
            }
        }
        assertFalse(new CameraPose(512, 392).holds(view));
    }

    @Test
    public void directionsFixYawFirst()
    {
        CameraPose camera = new CameraPose(0, 300);
        assertEquals("Rotate right 90°", camera.directionsTo(new CameraPose(512, 380)));
        assertEquals("Rotate left 90°", camera.directionsTo(new CameraPose(1536, 300)));
        assertEquals("Tilt camera higher", camera.directionsTo(new CameraPose(0, 380)));
        assertEquals("Hold the camera for a lap", camera.directionsTo(new CameraPose(10, 303)));
    }

    @Test
    public void viewsAboveTheStandardPitchLimitAreOutOfReach()
    {
        assertTrue(new CameraPose(0, CameraPose.STANDARD_MAX_PITCH).withinStandardPitch());
        assertTrue(CameraPose.fromViewKey(new CameraPose(0, CameraPose.STANDARD_MAX_PITCH).viewKey()).withinStandardPitch());
        assertFalse(CameraPose.fromViewKey("0:432").withinStandardPitch());
    }
}
