package com.mycompany.game3d;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Player {
    public float x, y, z, yaw;
    private float velY;
    private boolean onGround;
    private static final float SPEED=6f, GRAVITY=-20f, JUMP=8f;

    // ── Animacion squash and stretch ──
    private float scaleX = 1f, scaleY = 1f, scaleZ = 1f;
    private float squashTimer  = 0f;  // tiempo aplastado
    private float stretchTimer = 0f;  // tiempo estirado
    private boolean wasInAir   = false;
    private float fallSpeed    = 0f;  // velocidad de caida para calcular impacto

    private static final float SQUASH_TIME  = 0.12f;
    private static final float STRETCH_TIME = 0.10f;
    private static final float RECOVER_TIME = 0.25f;

    public Player(float x, float y, float z) {
        this.x=x; this.y=y; this.z=z;
    }

    public void update(float dt, InputHandler input, GameWorld world, Camera camera) {
        float mx=0, mz=0;

        if(input.isKeyDown(GLFW_KEY_W)){ mx+=(float)Math.sin(Math.toRadians(yaw)); mz-=(float)Math.cos(Math.toRadians(yaw)); }
        if(input.isKeyDown(GLFW_KEY_S)){ mx-=(float)Math.sin(Math.toRadians(yaw)); mz+=(float)Math.cos(Math.toRadians(yaw)); }
        if(input.isKeyDown(GLFW_KEY_A)){ mx-=(float)Math.cos(Math.toRadians(yaw)); mz-=(float)Math.sin(Math.toRadians(yaw)); }
        if(input.isKeyDown(GLFW_KEY_D)){ mx+=(float)Math.cos(Math.toRadians(yaw)); mz+=(float)Math.sin(Math.toRadians(yaw)); }

        float len=(float)Math.sqrt(mx*mx+mz*mz);
        if(len>0){ mx/=len; mz/=len; }

        if(input.isKeyDown(GLFW_KEY_SPACE)&&onGround){
            velY=JUMP;
            onGround=false;
            // Estirarse al saltar
            stretchTimer = STRETCH_TIME;
        }

        // Guardar si estaba en el aire antes
        wasInAir = !onGround;
        fallSpeed = velY; // guardar velocidad antes del impacto

        float prevX = x, prevZ = z;

        velY += GRAVITY * dt;
        x += mx * SPEED * dt;
        y += velY * dt;
        z += mz * SPEED * dt;

        float ground  = world.getHeightAt(x, z);
        float rockTop = world.getRockHeightAt(x, z);
        float surface = Math.max(ground, rockTop);

        if(y <= surface){
            y = surface;

            if(wasInAir && fallSpeed < -3f){
                squashTimer = SQUASH_TIME;
                float impact = Math.min(1f, Math.abs(fallSpeed) / 15f);
                scaleX = 1f + impact * 0.5f;
                scaleY = 1f - impact * 0.6f;
                scaleZ = 1f + impact * 0.5f;
            }

            velY = 0;
            onGround = true;
        } else {
            onGround = false;
        }

        // Colision horizontal solo si no estas encima de la roca
        if(world.hayColision(x, y, z)){
            x = prevX;
            z = prevZ;
        }
        // Limites del mundo
        float limit = (64 * 2f) / 2f - 1f;
        if(x >  limit) x =  limit;
        if(x < -limit) x = -limit;
        if(z >  limit) z =  limit;
        if(z < -limit) z = -limit;

        // ── Actualizar animacion ──
        updateAnimation(dt);
    }

    private void updateAnimation(float dt) {
        if(squashTimer > 0) {
            // Fase de aplaste
            squashTimer -= dt;
            float t = Math.max(0, squashTimer / SQUASH_TIME);
            // Interpolar de aplastado a normal
            scaleX = lerp(1f, scaleX, t);
            scaleY = lerp(1f, scaleY, t);
            scaleZ = lerp(1f, scaleZ, t);

            if(squashTimer <= 0) {
                // Despues del aplaste, rebotar hacia arriba (stretch)
                stretchTimer = RECOVER_TIME;
                scaleX = 0.85f;
                scaleY = 1.20f;
                scaleZ = 0.85f;
            }

        } else if(stretchTimer > 0) {
            // Fase de rebote / recuperacion
            stretchTimer -= dt;
            float t = Math.max(0, stretchTimer / RECOVER_TIME);
            scaleX = lerp(1f, scaleX, t);
            scaleY = lerp(1f, scaleY, t);
            scaleZ = lerp(1f, scaleZ, t);

            if(stretchTimer <= 0) {
                // Volver exactamente a normal
                scaleX = 1f;
                scaleY = 1f;
                scaleZ = 1f;
            }
        } else {
            // En el aire: estirarse levemente hacia abajo por gravedad
            if(!onGround && velY < -2f) {
                float stretch = Math.min(0.25f, Math.abs(velY) / 40f);
                scaleX = lerp(scaleX, 1f - stretch * 0.3f, 0.15f);
                scaleY = lerp(scaleY, 1f + stretch,        0.15f);
                scaleZ = lerp(scaleZ, 1f - stretch * 0.3f, 0.15f);
            } else {
                // Recuperar forma normal suavemente
                scaleX = lerp(scaleX, 1f, 0.2f);
                scaleY = lerp(scaleY, 1f, 0.2f);
                scaleZ = lerp(scaleZ, 1f, 0.2f);
            }
        }
    }

public void render() {
    glPushMatrix();
    glTranslatef(x, y, z);
    glTranslatef(0, 0.5f * scaleY, 0);
    glRotatef(yaw, 0, 1, 0);
    glRotatef(180f, 0, 1, 0);
    glScalef(0.5f * scaleX, 0.5f * scaleY, 0.5f * scaleZ);

    // Cuerpo del slime
    glColor3f(1f, 1f, 1f);
    try {
        ObjLoader.load("Slime.obj");
    } catch (Exception e) {
        System.out.println("ERROR: " + e.getMessage());
    }

    // Cara SIEMPRE encima del modelo
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_LIGHTING);

    // Ojo izquierdo blanco
    glColor3f(1f, 1f, 1f);
    drawEllipse(-0.20f, 0.22f, 0.62f, 0.14f, 0.16f, 20);
    // Ojo derecho blanco
    drawEllipse( 0.20f, 0.22f, 0.62f, 0.14f, 0.16f, 20);
    // Pupila izquierda
    glColor3f(0.05f, 0.05f, 0.05f);
    drawEllipse(-0.20f, 0.21f, 0.64f, 0.08f, 0.10f, 16);
    // Pupila derecha
    drawEllipse( 0.20f, 0.21f, 0.64f, 0.08f, 0.10f, 16);
    // Brillo izquierdo
    glColor3f(1f, 1f, 1f);
    drawEllipse(-0.14f, 0.26f, 0.65f, 0.03f, 0.04f, 10);
    // Brillo derecho
    drawEllipse( 0.26f, 0.26f, 0.65f, 0.03f, 0.04f, 10);
    // Sonrisa
    glColor3f(0.90f, 0.55f, 0.45f);
    drawSmile(0f, 0.05f, 0.63f, 0.18f, 0.09f, 16);

    glEnable(GL_DEPTH_TEST);
    glEnable(GL_LIGHTING);

    glPopMatrix();
}

public void renderShadow(float sunX, float sunZ) {

    glDisable(GL_LIGHTING);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    // ==========================
    // ALTURA DEL SLIME
    // ==========================
    float height = Math.max(0f, y);

    // Mientras más alto esté, más pequeña la sombra
    float jumpScale = 1.0f - Math.min(height * 0.12f, 0.55f);

    // Transparencia también disminuye
    float alpha = 0.42f - Math.min(height * 0.04f, 0.20f);

    glColor4f(0f, 0f, 0f, alpha);

    // ==========================
    // DIRECCION DEL SOL
    // ==========================
    float dirX = x - sunX;
    float dirZ = z - sunZ;

    float dist = (float)Math.sqrt(dirX * dirX + dirZ * dirZ);

    if (dist < 0.001f)
        dist = 1f;

    float dx = dirX / dist;
    float dz = dirZ / dist;

    float px = -dz;
    float pz = dx;

    // ==========================
    // ESCALA DE LA SOMBRA
    // ==========================
    float radius = 0.75f * scaleX * jumpScale;

    // Más estirada conforme sube
    float stretch = 1.0f + height * 0.05f;

    glBegin(GL_POLYGON);

    // ==========================
    // PICO DEL SLIME
    // ==========================
    glVertex3f(
        x + dx * radius * 2.4f,
        0.02f,
        z + dz * radius * 2.4f
    );

    int seg = 40;

    for (int i = 0; i <= seg; i++) {

        float a = (float)(Math.PI * i / seg);

        float sx = (float)Math.cos(a);
        float sz = (float)Math.sin(a);

        float worldX =
                x
              - dx * sz * radius * stretch
              + px * sx * radius;

        float worldZ =
                z
              - dz * sz * radius * stretch
              + pz * sx * radius;

        glVertex3f(worldX, 0.02f, worldZ);
    }

    glEnd();

    glDisable(GL_BLEND);
    glEnable(GL_LIGHTING);
}

    private void drawEllipse(float cx, float cy, float cz, float rw, float rh, int seg) {
        glBegin(GL_TRIANGLE_FAN);
        glVertex3f(cx, cy, cz);
        for (int i = 0; i <= seg; i++) {
            float a = (float)(Math.PI * 2 * i / seg);
            glVertex3f(cx + (float)Math.cos(a)*rw, cy + (float)Math.sin(a)*rh, cz);
        }
        glEnd();
    }

    private void drawSmile(float cx, float cy, float cz, float rw, float rh, int seg) {
        glBegin(GL_TRIANGLE_STRIP);
        for (int i = 0; i <= seg; i++) {
            float a = (float)(Math.PI * i / seg);
            float cosA = (float)Math.cos(a);
            float sinA = -(float)Math.sin(a);
            glVertex3f(cx + cosA * rw,            cy + sinA * rh - 0.04f,            cz);
            glVertex3f(cx + cosA * (rw*0.65f),    cy + sinA * (rh*0.65f) - 0.04f,   cz);
        }
        glEnd();
    }

    private float lerp(float a, float b, float t) { return a + (b-a)*t; }
}