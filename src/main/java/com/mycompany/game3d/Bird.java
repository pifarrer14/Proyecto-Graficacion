package com.mycompany.game3d;

import static org.lwjgl.opengl.GL11.*;

public class Bird {

    // Posicion actual
    private float x, y, z;

    // Centro de la ruta circular
    private float centerX, centerZ;

    // Radio y velocidad de la ruta
    private float radius;
    private float speed;
    private float angle;

    // Altura de vuelo
    private float height;

    // Aleteo
    private float wingAngle = 0f;
    private float wingSpeed = 3.0f;
    private boolean wingUp = true;

    public Bird(float centerX, float centerZ, float radius, float height, float speed, float startAngle) {
        this.centerX = centerX;
        this.centerZ  = centerZ;
        this.radius   = radius;
        this.height   = height;
        this.speed    = speed;
        this.angle    = startAngle;
        this.x = centerX + (float)Math.cos(angle) * radius;
        this.y = height;
        this.z = centerZ + (float)Math.sin(angle) * radius;
    }

    public void update(float dt) {
        // Mover en circulo
        angle += speed * dt;
        x = centerX + (float)Math.cos(angle) * radius;
        z = centerZ + (float)Math.sin(angle) * radius;
        y = height + (float)Math.sin(angle * 2f) * 2f; // sube y baja suavemente

        // Aleteo
        if (wingUp) {
            wingAngle += wingSpeed * dt * 60f;
            if (wingAngle > 35f) wingUp = false;
        } else {
            wingAngle -= wingSpeed * dt * 60f;
            if (wingAngle < -15f) wingUp = true;
        }
    }

    public void render() {
        glDisable(GL_LIGHTING);
        glPushMatrix();
        glTranslatef(x, y, z);

        // Rotar para mirar hacia donde vuela
        float dir = (float)Math.toDegrees(Math.atan2(
            -(float)Math.sin(angle),
            -(float)Math.cos(angle)
        ));
        glRotatef(dir, 0, 1, 0);
        glScalef(0.8f, 0.8f, 0.8f);

        // Cuerpo del pajaro
        glColor3f(0.15f, 0.10f, 0.10f);
        drawBox(0, 0, 0, 0.4f, 0.15f, 0.6f);

        // Cabeza
        glColor3f(0.15f, 0.10f, 0.10f);
        drawBox(0, 0.1f, 0.35f, 0.2f, 0.18f, 0.2f);

        // Pico
        glColor3f(0.8f, 0.6f, 0.1f);
        drawBox(0, 0.08f, 0.52f, 0.06f, 0.05f, 0.12f);

        // Ala izquierda (con aleteo)
        glColor3f(0.20f, 0.15f, 0.15f);
        glPushMatrix();
        glTranslatef(-0.2f, 0, 0);
        glRotatef(wingAngle, 0, 0, 1);
        drawBox(-0.4f, 0, 0, 0.8f, 0.05f, 0.4f);
        glPopMatrix();

        // Ala derecha (con aleteo opuesto)
        glPushMatrix();
        glTranslatef(0.2f, 0, 0);
        glRotatef(-wingAngle, 0, 0, 1);
        drawBox(0.4f, 0, 0, 0.8f, 0.05f, 0.4f);
        glPopMatrix();

        // Cola
        glColor3f(0.12f, 0.08f, 0.08f);
        drawBox(0, -0.05f, -0.35f, 0.25f, 0.08f, 0.2f);

        glPopMatrix();
        glEnable(GL_LIGHTING);
    }

    public void renderShadow() {
        glDisable(GL_LIGHTING);
        glDisable(GL_COLOR_MATERIAL);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // La sombra se hace mas pequeña y desplazada segun la altura
        float scale  = Math.max(0.1f, 1f - y / 60f);
        float offX   =  (y / 10f) * 1.5f; // desplazamiento segun altura y sol
        float offZ   =  (y / 10f) * 0.3f;

        glColor4f(0f, 0f, 0f, 0.3f * scale);

        // Sombra en forma de pajaro (simplificada como cruz)
        glPushMatrix();
        glTranslatef(x + offX, 0.02f, z + offZ);

        float dir = (float)Math.toDegrees(Math.atan2(
            -(float)Math.sin(angle),
            -(float)Math.cos(angle)
        ));
        glRotatef(dir, 0, 1, 0);
        glScalef(scale, 1f, scale);

        // Cuerpo sombra
        glBegin(GL_QUADS);
        glNormal3f(0,1,0);
        glVertex3f(-0.15f, 0, -0.5f);
        glVertex3f( 0.15f, 0, -0.5f);
        glVertex3f( 0.15f, 0,  0.5f);
        glVertex3f(-0.15f, 0,  0.5f);
        glEnd();

        // Alas sombra
        glBegin(GL_QUADS);
        glVertex3f(-1.2f, 0, -0.1f);
        glVertex3f( 1.2f, 0, -0.1f);
        glVertex3f( 1.2f, 0,  0.1f);
        glVertex3f(-1.2f, 0,  0.1f);
        glEnd();

        glPopMatrix();
        glEnable(GL_COLOR_MATERIAL);
        glEnable(GL_LIGHTING);
    }

    private void drawBox(float cx, float cy, float cz, float w, float h, float d) {
        float hw=w/2, hh=h/2, hd=d/2;
        glBegin(GL_QUADS);
        glNormal3f(0,0,1);  glVertex3f(cx-hw,cy-hh,cz+hd); glVertex3f(cx+hw,cy-hh,cz+hd); glVertex3f(cx+hw,cy+hh,cz+hd); glVertex3f(cx-hw,cy+hh,cz+hd);
        glNormal3f(0,0,-1); glVertex3f(cx+hw,cy-hh,cz-hd); glVertex3f(cx-hw,cy-hh,cz-hd); glVertex3f(cx-hw,cy+hh,cz-hd); glVertex3f(cx+hw,cy+hh,cz-hd);
        glNormal3f(-1,0,0); glVertex3f(cx-hw,cy-hh,cz-hd); glVertex3f(cx-hw,cy-hh,cz+hd); glVertex3f(cx-hw,cy+hh,cz+hd); glVertex3f(cx-hw,cy+hh,cz-hd);
        glNormal3f(1,0,0);  glVertex3f(cx+hw,cy-hh,cz+hd); glVertex3f(cx+hw,cy-hh,cz-hd); glVertex3f(cx+hw,cy+hh,cz-hd); glVertex3f(cx+hw,cy+hh,cz+hd);
        glNormal3f(0,1,0);  glVertex3f(cx-hw,cy+hh,cz+hd); glVertex3f(cx+hw,cy+hh,cz+hd); glVertex3f(cx+hw,cy+hh,cz-hd); glVertex3f(cx-hw,cy+hh,cz-hd);
        glNormal3f(0,-1,0); glVertex3f(cx-hw,cy-hh,cz-hd); glVertex3f(cx+hw,cy-hh,cz-hd); glVertex3f(cx+hw,cy-hh,cz+hd); glVertex3f(cx-hw,cy-hh,cz+hd);
        glEnd();
    }
}