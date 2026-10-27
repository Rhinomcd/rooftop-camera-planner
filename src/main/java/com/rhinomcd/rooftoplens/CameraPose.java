package com.rhinomcd.rooftoplens;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarClientID;

/** Yaw and pitch in JAU11 (2048 per turn), zoom as the client's camera zoom varc. */
final class CameraPose
{
    static final int YAW_UNITS = 2048;
    static final int STANDARD_MAX_PITCH = 383;
    private static final int YAW_STEP = 64;
    private static final int PITCH_STEP = 16;
    private static final int ZOOM_STEP = 32;
    private static final int YAW_SLACK = 24;
    private static final int PITCH_SLACK = 7;
    private static final int ZOOM_SLACK = 15;

    final int yaw;
    final int pitch;
    final int zoom;

    CameraPose(int yaw, int pitch, int zoom)
    {
        this.yaw = Math.floorMod(yaw, YAW_UNITS);
        this.pitch = pitch;
        this.zoom = zoom;
    }

    /**
     * RuneLite reports camera angles in JAU14. Pitch above the game's standard limit (expanded with the Camera
     * plugin's pitch relaxer) is read as the limit, because the game pulls it back there mid-course on its own.
     */
    static CameraPose read(Client client)
    {
        int zoomVarc = client.isResized() ? VarClientID.CAMERA_ZOOM_BIG : VarClientID.CAMERA_ZOOM_SMALL;
        return new CameraPose(client.getCameraYawTarget() >> 3,
            Math.min(client.getCameraPitchTarget() >> 3, STANDARD_MAX_PITCH), client.getVarcIntValue(zoomVarc));
    }

    /** Within the slack of another pose. Slack is under half a view step, so a pose that holds a snapped view shares its key. */
    boolean holds(CameraPose start)
    {
        return Math.abs(yawDelta(start.yaw)) <= YAW_SLACK
            && Math.abs(pitch - start.pitch) <= PITCH_SLACK
            && Math.abs(zoom - start.zoom) <= ZOOM_SLACK;
    }

    /** Shortest signed rotation from this yaw to the target yaw. */
    int yawDelta(int targetYaw)
    {
        return Math.floorMod(targetYaw - yaw + YAW_UNITS / 2, YAW_UNITS) - YAW_UNITS / 2;
    }

    String directionsTo(CameraPose goal)
    {
        int yawOff = yawDelta(goal.yaw);
        if (Math.abs(yawOff) > YAW_SLACK)
        {
            return "Rotate " + (yawOff > 0 ? "right " : "left ") + Math.round(Math.abs(yawOff) * 360f / YAW_UNITS) + "\u00b0";
        }
        if (Math.abs(goal.pitch - pitch) > PITCH_SLACK)
        {
            return goal.pitch > pitch ? "Tilt camera higher" : "Tilt camera lower";
        }
        if (Math.abs(goal.zoom - zoom) > ZOOM_SLACK)
        {
            return goal.zoom > zoom ? "Zoom in a little" : "Zoom out a little";
        }
        return "Hold the camera for a lap";
    }

    String viewKey()
    {
        return snap(yaw, YAW_STEP) % YAW_UNITS + ":" + snap(pitch, PITCH_STEP) + ":" + snap(zoom, ZOOM_STEP);
    }

    /** False for views saved before pitch was capped at the standard limit. */
    boolean withinStandardPitch()
    {
        return pitch <= snap(STANDARD_MAX_PITCH, PITCH_STEP);
    }

    static CameraPose fromViewKey(String key)
    {
        String[] parts = key.split(":");
        return new CameraPose(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    private static int snap(int value, int step)
    {
        return Math.round(value / (float) step) * step;
    }
}
