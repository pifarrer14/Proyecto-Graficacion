package com.mycompany.game3d;

import static org.lwjgl.opengl.GL11.*;
import java.nio.*;

public class Camera {

    private final Player    player;
    private final GameWorld world;

    // Solo pitch controlado por mouse (vertical)
    private float pitch  = 20f;  // vertical
    private float hOffset = 0f;  // horizontal relativo al jugador
    private static final float MIN_PITCH   =  0f;
    private static final float MAX_PITCH   = 30f;
    private static final float MIN_H       = -30f; // limite izquierda
    private static final float MAX_H       =  30f; // limite derecha
    private static final float DIST        =  6.0f;
    private static final float MIN_CAM_Y   =  0.5f;

    public Camera(Player player, GameWorld world) {
        this.player = player;
        this.world  = world;
    }

 public void update(float dt, InputHandler input) {
    // Mouse Y mueve vertical
    pitch   += (float)(input.getMouseDY() * 0.15f);
    pitch    = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));

    // Mouse X acumula el offset horizontal
    hOffset += (float)(input.getMouseDX() * 0.15f);

    // Si supera los limites, el exceso se transfiere al yaw del jugador
    if (hOffset > MAX_H) {
        player.yaw += (hOffset - MAX_H);
        hOffset = MAX_H;
    } else if (hOffset < MIN_H) {
        player.yaw += (hOffset - MIN_H);
        hOffset = MIN_H;
    }

    // El jugador siempre mira hacia donde apunta la camara
    player.yaw += hOffset * dt * 5f;
    hOffset    -= hOffset * dt * 5f;
}

    public float getYaw() {
        return player.yaw; // la camara sigue el yaw del jugador
    }

    public void applyView() {
        // Yaw total = yaw del jugador + offset horizontal del mouse
        float totalYaw = player.yaw + hOffset;

        float radYaw   = (float) Math.toRadians(totalYaw);
        float radPitch = (float) Math.toRadians(pitch);

        float cosP = (float) Math.cos(radPitch);
        float sinP = (float) Math.sin(radPitch);
        float cosY = (float) Math.cos(radYaw);
        float sinY = (float) Math.sin(radYaw);

        float camX = player.x - sinY * cosP * DIST;
        float camY = player.y + sinP * DIST;
        float camZ = player.z + cosY * cosP * DIST;

        float groundY = world.getHeightAt(camX, camZ);
        if (camY < groundY + MIN_CAM_Y) {
            camY = groundY + MIN_CAM_Y;
        }

        float targetX = player.x;
        float targetY = player.y + 1.0f;
        float targetZ = player.z;

        lookAt(camX, camY, camZ, targetX, targetY, targetZ);
    }

    private void lookAt(float ex, float ey, float ez,
                        float tx, float ty, float tz) {
        float fx=tx-ex, fy=ty-ey, fz=tz-ez;
        float fl=(float)Math.sqrt(fx*fx+fy*fy+fz*fz);
        fx/=fl; fy/=fl; fz/=fl;

        float sx = fy*0 - fz*1;
        float sy = fz*0 - fx*0;
        float sz = fx*1 - fy*0;
        float sl=(float)Math.sqrt(sx*sx+sy*sy+sz*sz);
        if(sl>0){ sx/=sl; sy/=sl; sz/=sl; }

        float ux=sy*fz-sz*fy;
        float uy=sz*fx-sx*fz;
        float uz=sx*fy-sy*fx;

        FloatBuffer m = ByteBuffer.allocateDirect(64)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m.put(new float[]{
            sx, ux, -fx, 0,
            sy, uy, -fy, 0,
            sz, uz, -fz, 0,
             0,  0,   0, 1
        }).flip();
        glLoadMatrixf(m);
        glTranslatef(-ex, -ey, -ez);
    }
    public float getHOffset() {
    return hOffset;
}
}