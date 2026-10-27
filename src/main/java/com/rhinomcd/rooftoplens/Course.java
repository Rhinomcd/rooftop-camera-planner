package com.rhinomcd.rooftoplens;

import java.util.Arrays;
import net.runelite.api.gameval.ObjectID;

enum Course
{
    DRAYNOR("Draynor Village", new int[] {
        ObjectID.ROOFTOPS_DRAYNOR_WALLCLIMB, ObjectID.ROOFTOPS_DRAYNOR_TIGHTROPE_1,
        ObjectID.ROOFTOPS_DRAYNOR_TIGHTROPE_2, ObjectID.ROOFTOPS_DRAYNOR_WALLCROSSING,
        ObjectID.ROOFTOPS_DRAYNOR_WALLSCRAMBLE, ObjectID.ROOFTOPS_DRAYNOR_LEAPDOWN,
        ObjectID.ROOFTOPS_DRAYNOR_CRATE
    }),
    AL_KHARID("Al Kharid", new int[] {
        ObjectID.ROOFTOPS_KHARID_WALLCLIMB, ObjectID.ROOFTOPS_KHARID_TIGHTROPE_1,
        ObjectID.ROOFTOPS_KHARID_ROPE_SWING, ObjectID.ROOFTOPS_KHARID_SLIDE_SIDE,
        ObjectID.ROOFTOPS_KHARID_BAMBOO_TREE_TOP, ObjectID.ROOFTOPS_KHARID_WALLCLIMB_2,
        ObjectID.ROOFTOPS_KHARID_TIGHTROPE_4, ObjectID.ROOFTOPS_KHARID_LEAPDOWN
    }),
    VARROCK("Varrock", new int[] {
        ObjectID.ROOFTOPS_VARROCK_WALLCLIMB, ObjectID.ROOFTOPS_VARROCK_CLOTHESLINE,
        ObjectID.ROOFTOPS_VARROCK_LEAPTORUINS, ObjectID.ROOFTOPS_VARROCK_WALLSWING,
        ObjectID.ROOFTOPS_VARROCK_WALLSCRAMBLE, ObjectID.ROOFTOPS_VARROCK_LEAPTOBALCONY,
        ObjectID.ROOFTOPS_VARROCK_LEAPDOWN, ObjectID.ROOFTOPS_VARROCK_STEPUPROOF,
        ObjectID.ROOFTOPS_VARROCK_FINISH
    }),
    CANIFIS("Canifis", new int[] {
        ObjectID.ROOFTOPS_CANIFIS_START_TREE, ObjectID.ROOFTOPS_CANIFIS_JUMP,
        ObjectID.ROOFTOPS_CANIFIS_JUMP_2, ObjectID.ROOFTOPS_CANIFIS_JUMP_5,
        ObjectID.ROOFTOPS_CANIFIS_JUMP_3, ObjectID.ROOFTOPS_CANIFIS_POLEVAULT,
        ObjectID.ROOFTOPS_CANIFIS_JUMP_4, ObjectID.ROOFTOPS_CANIFIS_LEAPDOWN
    }),
    FALADOR("Falador", new int[][] {
        { ObjectID.ROOFTOPS_FALADOR_WALLCLIMB }, { ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_1 },
        { ObjectID.ROOFTOPS_FALADOR_HANDHOLDS_START }, { ObjectID.ROOFTOPS_FALADOR_GAP_1 },
        { ObjectID.ROOFTOPS_FALADOR_GAP_2 }, { ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_2 },
        { ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_3 }, { ObjectID.ROOFTOPS_FALADOR_GAP_3 },
        { ObjectID.ROOFTOPS_FALADOR_LEDGE_1 }, { ObjectID.ROOFTOPS_FALADOR_LEDGE_2 },
        { ObjectID.ROOFTOPS_FALADOR_LEDGE_3A, ObjectID.ROOFTOPS_FALADOR_LEDGE_3B },
        { ObjectID.ROOFTOPS_FALADOR_LEDGE_4 }, { ObjectID.ROOFTOPS_FALADOR_EDGE }
    }),
    SEERS("Seers' Village", new int[] {
        ObjectID.ROOFTOPS_SEERS_WALLCLIMB, ObjectID.ROOFTOPS_SEERS_JUMP,
        ObjectID.ROOFTOPS_SEERS_TIGHTROPE, ObjectID.ROOFTOPS_SEERS_JUMP_1,
        ObjectID.ROOFTOPS_SEERS_JUMP_2, ObjectID.ROOFTOPS_SEERS_LEAPDOWN
    }),
    POLLNIVNEACH("Pollnivneach", new int[] {
        ObjectID.ROOFTOPS_POLLNIVNEACH_BASKET, ObjectID.ROOFTOPS_POLLNIVNEACH_MARKETSTALL,
        ObjectID.ROOFTOPS_POLLNIVNEACH_HANGINGBANNER, ObjectID.ROOFTOPS_POLLNIVNEACH_GAP,
        ObjectID.ROOFTOPS_POLLNIVNEACH_TREE, ObjectID.ROOFTOPS_POLLNIVNEACH_WALLCLIMB,
        ObjectID.ROOFTOPS_POLLNIVNEACH_MONKEYBARS_START, ObjectID.ROOFTOPS_POLLNIVNEACH_TREETOP,
        ObjectID.ROOFTOPS_POLLNIVNEACH_LINE
    }),
    RELLEKKA("Rellekka", new int[] {
        ObjectID.ROOFTOPS_RELLEKKA_WALLCLIMB, ObjectID.ROOFTOPS_RELLEKKA_GAP_1,
        ObjectID.ROOFTOPS_RELLEKKA_TIGHTROPE_1, ObjectID.ROOFTOPS_RELLEKKA_GAP_2,
        ObjectID.ROOFTOPS_RELLEKKA_GAP_3, ObjectID.ROOFTOPS_RELLEKKA_TIGHTROPE_3,
        ObjectID.ROOFTOPS_RELLEKKA_DROPOFF
    }),
    ARDOUGNE("Ardougne", new int[] {
        ObjectID.ROOFTOPS_ARDY_WALLCLIMB, ObjectID.ROOFTOPS_ARDY_JUMP,
        ObjectID.ROOFTOPS_ARDY_PLANK, ObjectID.ROOFTOPS_ARDY_JUMP_2,
        ObjectID.ROOFTOPS_ARDY_JUMP_3, ObjectID.ROOFTOPS_ARDY_WALLCROSSING,
        ObjectID.ROOFTOPS_ARDY_JUMP_4
    });

    final String displayName;
    private final int[][] steps;

    Course(String displayName, int[] obstacles)
    {
        this(displayName, Arrays.stream(obstacles).mapToObj(id -> new int[] { id }).toArray(int[][]::new));
    }

    Course(String displayName, int[][] steps)
    {
        this.displayName = displayName;
        this.steps = steps;
    }

    /** Index of the step this object belongs to, or -1. Alternate objects (Falador's ledges) share a step. */
    int stepOf(int objectId)
    {
        for (int i = 0; i < steps.length; i++)
        {
            for (int id : steps[i])
            {
                if (id == objectId)
                {
                    return i;
                }
            }
        }
        return -1;
    }

    int stepCount()
    {
        return steps.length;
    }

    static Course withObstacle(int objectId)
    {
        return Arrays.stream(values()).filter(course -> course.stepOf(objectId) >= 0).findFirst().orElse(null);
    }
}
