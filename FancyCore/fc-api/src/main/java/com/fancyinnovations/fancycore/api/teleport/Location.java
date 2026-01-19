package com.fancyinnovations.fancycore.api.teleport;

import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

public record Location(
        @SerializedName("world_name") String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    public Vector3d positionVec() {
        return new Vector3d(x, y, z);
    }

    public Vector3f rotationVec() {
        return new Vector3f(pitch, yaw, 0f);
    }
}
