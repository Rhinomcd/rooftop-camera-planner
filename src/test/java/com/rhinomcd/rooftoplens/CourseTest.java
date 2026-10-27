package com.rhinomcd.rooftoplens;

import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CourseTest
{
    @Test
    public void alternateObjectsShareAStep()
    {
        assertEquals(Course.FALADOR.stepOf(ObjectID.ROOFTOPS_FALADOR_LEDGE_3A),
            Course.FALADOR.stepOf(ObjectID.ROOFTOPS_FALADOR_LEDGE_3B));
        assertEquals(Course.RELLEKKA, Course.withObstacle(ObjectID.ROOFTOPS_RELLEKKA_GAP_1));
        assertEquals(6, Course.RELLEKKA.stepOf(ObjectID.ROOFTOPS_RELLEKKA_DROPOFF));
    }
}
