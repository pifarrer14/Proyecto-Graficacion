package com.mycompany.game3d;

import static org.lwjgl.opengl.GL11.*;
import java.util.*;

public class GameWorld {

    private static final int   GRID = 64;
    private static final float CELL = 2f;
    private static final float SIZE = GRID * CELL;

    private final float sunX = -200f;
    private final float sunY =  160f;
    private final float sunZ =   80f;

    private float[][] hmap;
    private List<float[]> trees  = new ArrayList<>();
    private List<float[]> rocks  = new ArrayList<>();
    private List<float[]> houses = new ArrayList<>();
    private List<Bird>    birds  = new ArrayList<>();
    private List<float[]> grass  = new ArrayList<>();
    private List<float[]> patches = new ArrayList<>(); // centros de parches

    // Rotaciones aleatorias para las rocas
    private List<Float> rockRot = new ArrayList<>();

    public GameWorld() {
        buildHeightmap();
        placeObjects();
        placeBirds();
    }

    public float getSunX() { return sunX; }
    public float getSunZ() { return sunZ; }

    private void buildHeightmap() {
        hmap = new float[GRID+1][GRID+1];
        for (int z = 0; z <= GRID; z++)
            for (int x = 0; x <= GRID; x++)
                hmap[z][x] = 0f;
    }

    public float getHeightAt(float wx, float wz) { return 0f; }

    private void placeObjects() {
        Random r = new Random(77);
        for (int i = 0; i < 80; i++) {
            float px = (r.nextFloat() - .5f) * SIZE;
            float pz = (r.nextFloat() - .5f) * SIZE;
            if (Math.sqrt(px*px + pz*pz) < 8) continue;
            trees.add(new float[]{px, 0f, pz, .8f + r.nextFloat() * 1.2f});
        }
        for (int i = 0; i < 40; i++) {
            float px = (r.nextFloat() - .5f) * SIZE;
            float pz = (r.nextFloat() - .5f) * SIZE;
            if (Math.sqrt(px*px + pz*pz) < 8) continue;
            rocks.add(new float[]{px, 0f, pz, .3f + r.nextFloat() * .8f});
            rockRot.add(r.nextFloat() * 360f); // rotacion aleatoria
        }
        for (int i = 0; i < 6; i++) {
            float ang = (float)(Math.PI * 2 * i / 6);
            float rad = 20f + r.nextFloat() * 10f;
            float px  = (float)(Math.cos(ang) * rad);
            float pz  = (float)(Math.sin(ang) * rad);
            houses.add(new float[]{px, 0f, pz, 1f + r.nextFloat() * .5f});
        }
// Parches de hierba optimizados
Random rp = new Random(55);
for (int p = 0; p < 12; p++) {
    float pcx = (rp.nextFloat() - .5f) * SIZE * 0.8f;
    float pcz = (rp.nextFloat() - .5f) * SIZE * 0.8f;
    if (Math.sqrt(pcx*pcx + pcz*pcz) < 10) continue;

    float pradius = 5f + rp.nextFloat() * 7f;
    int count = (int)(pradius * 4); // muchos menos mechones

    for (int i = 0; i < count; i++) {
        float angle = rp.nextFloat() * (float)(Math.PI * 2);
        float dist2 = pradius * (float)Math.sqrt(rp.nextFloat());
        float gx = pcx + (float)Math.cos(angle) * dist2;
        float gz = pcz + (float)Math.sin(angle) * dist2;

        boolean ocupado = false;
        for (float[] t : trees) {
            float dx2=gx-t[0], dz2=gz-t[2];
            if(Math.sqrt(dx2*dx2+dz2*dz2) < t[3]*0.5f){ ocupado=true; break; }
        }
        for (float[] h : houses) {
            if(Math.abs(gx-h[0]) < h[3]*2f && Math.abs(gz-h[2]) < h[3]*2f){ ocupado=true; break; }
        }
        if(ocupado) continue;

        float scale = 0.35f + rp.nextFloat() * 0.55f;
        float rot   = rp.nextFloat() * 360f;
        grass.add(new float[]{gx, 0f, gz, scale, rot});
    }
}
    }

    private void placeBirds() {
        birds.add(new Bird(  0f,   0f, 40f, 30f, 0.4f, 0f));
        birds.add(new Bird( 20f,  10f, 25f, 45f, 0.6f, 1f));
        birds.add(new Bird(-15f,  20f, 35f, 35f, 0.5f, 2f));
        birds.add(new Bird( 10f, -20f, 30f, 50f, 0.3f, 3f));
        birds.add(new Bird(-10f, -10f, 20f, 40f, 0.7f, 4f));
    }

    public void update(float dt) {
        for (Bird b : birds) b.update(dt);
    }

public void render(float playerX, float playerZ) {
    renderSky();
    renderTerrain();
    renderWalls();
    for (float[] hs : houses) renderHouseShadow(hs[0], hs[2], hs[3]);
    for (int i = 0; i < trees.size(); i++) renderTreeShadow(trees.get(i)[0], trees.get(i)[2], trees.get(i)[3]);
    for (int i = 0; i < rocks.size(); i++) renderRockShadow(rocks.get(i)[0], rocks.get(i)[2], rocks.get(i)[3]);
    // Hierba solo cerca del jugador
    for (float[] g : grass) {
        float dx = g[0]-playerX, dz = g[2]-playerZ;
        if (dx*dx + dz*dz > 30f*30f) continue; // solo 30 unidades de distancia
        renderGrassShadow(g[0], g[2], g[3]);
    }
    for (Bird b : birds) b.renderShadow();
    for (float[] t  : trees)  renderTree(t[0], t[1], t[2], t[3]);
    for (int i = 0; i < rocks.size(); i++) {
        float[] ro = rocks.get(i);
        renderRock(ro[0], ro[1], ro[2], ro[3], rockRot.get(i));
    }
    for (float[] hs : houses) renderHouse(hs[0], hs[1], hs[2], hs[3]);
    for (float[] g : grass) {
        float dx = g[0]-playerX, dz = g[2]-playerZ;
        if (dx*dx + dz*dz > 30f*30f) continue;
        renderGrass(g[0], g[1], g[2], g[3], g[4]);
    }
    for (Bird b : birds) b.render();
}

    // ── Cielo ─────────────────────────────────────────────────────────────
    private void renderSky() {
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        // Guardar matrices y cambiar a proyeccion 2D
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(-1, 1, -1, 1, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // Quad 2D con degradado vertical - sin esquinas ni artefactos
        glBegin(GL_QUADS);
        glColor3f(.55f, .78f, .95f); glVertex2f(-1f, -1f);
        glColor3f(.55f, .78f, .95f); glVertex2f( 1f, -1f);
        glColor3f(.05f, .18f, .58f); glVertex2f( 1f,  1f);
        glColor3f(.05f, .18f, .58f); glVertex2f(-1f,  1f);
        glEnd();

        // Restaurar matrices
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);

        glEnable(GL_DEPTH_TEST);

        // Sol
        drawSun(sunX, sunY, sunZ);
        glEnable(GL_LIGHTING);
    }

    // ── Sol 3D mejorado ───────────────────────────────────────────────────
    private void drawSun(float sx, float sy, float sz) {
        glDisable(GL_LIGHTING);
        glPushMatrix();
        glTranslatef(sx, sy, sz);

        int stacks = 32, slices = 32;
        float r = 25f;

        // Nucleo blanco brillante
        glColor3f(1.0f, 1.0f, 0.85f);
        drawSphere(r * 0.5f, stacks, slices);

        // Esfera principal
        glColor3f(1.0f, 0.92f, 0.15f);
        drawSphere(r, stacks, slices);

        // Halos semitransparentes
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1.0f, 0.85f, 0.2f, 0.20f);
        drawSphere(r * 1.4f, stacks, slices);
        glColor4f(1.0f, 0.75f, 0.1f, 0.08f);
        drawSphere(r * 1.9f, stacks, slices);

        glPopMatrix();
        glEnable(GL_LIGHTING);
    }

    private void drawSphere(float r, int stacks, int slices) {
        for (int i = 0; i < stacks; i++) {
            float lat0 = (float)(Math.PI*(-0.5+(double)i/stacks));
            float lat1 = (float)(Math.PI*(-0.5+(double)(i+1)/stacks));
            float y0=  (float)Math.sin(lat0), yr0=(float)Math.cos(lat0);
            float y1=  (float)Math.sin(lat1), yr1=(float)Math.cos(lat1);
            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= slices; j++) {
                float lng=(float)(2*Math.PI*(double)j/slices);
                float cL=(float)Math.cos(lng), sL=(float)Math.sin(lng);
                glNormal3f(cL*yr0,y0,sL*yr0); glVertex3f(cL*yr0*r,y0*r,sL*yr0*r);
                glNormal3f(cL*yr1,y1,sL*yr1); glVertex3f(cL*yr1*r,y1*r,sL*yr1*r);
            }
            glEnd();
        }
    }

    // ── Paredes ───────────────────────────────────────────────────────────
    private void renderWalls() {
        float half = SIZE / 2f;
        float wallH = 20f;
        glDisable(GL_LIGHTING);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        // Paredes semitransparentes para mezclar con el cielo
        glColor4f(0.40f, 0.65f, 0.92f, 0.85f);
        glBegin(GL_QUADS);
        glNormal3f(0,0,1);
        glVertex3f(-half,0,-half); glVertex3f(half,0,-half);
        glVertex3f(half,wallH,-half); glVertex3f(-half,wallH,-half);
        glNormal3f(0,0,-1);
        glVertex3f(half,0,half); glVertex3f(-half,0,half);
        glVertex3f(-half,wallH,half); glVertex3f(half,wallH,half);
        glNormal3f(1,0,0);
        glVertex3f(-half,0,-half); glVertex3f(-half,0,half);
        glVertex3f(-half,wallH,half); glVertex3f(-half,wallH,-half);
        glNormal3f(-1,0,0);
        glVertex3f(half,0,half); glVertex3f(half,0,-half);
        glVertex3f(half,wallH,-half); glVertex3f(half,wallH,half);
        glEnd();
        glEnable(GL_LIGHTING);
    }

    // ── Terreno ───────────────────────────────────────────────────────────
    private void renderTerrain() {
        float half = SIZE / 2f;
        // Piso verde
        glColor3f(0.22f, 0.68f, 0.12f);
        glBegin(GL_QUADS);
        glNormal3f(0f, 1f, 0f);
        glVertex3f(-half,0f, half); glVertex3f(half,0f, half);
        glVertex3f( half,0f,-half); glVertex3f(-half,0f,-half);
        glEnd();

        // Cuadricula muy sutil
        glDisable(GL_LIGHTING);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0f, 0f, 0f, 0.03f); // muy transparente
        glLineWidth(1.0f);
        glBegin(GL_LINES);
        for (int i = -GRID/2; i <= GRID/2; i++) {
            float p = i * CELL;
            glVertex3f(p, 0.01f,-half); glVertex3f(p, 0.01f, half);
            glVertex3f(-half,0.01f,p);  glVertex3f( half,0.01f,p);
        }
        glEnd();
        glEnable(GL_LIGHTING);
    }

    // ── Arbol HD ──────────────────────────────────────────────────────────
    private void renderTree(float x, float y, float z, float s) {
        glPushMatrix();
        glTranslatef(x, y, z);
        glScalef(s, s, s);

        // Raices
        glColor3f(.38f, .22f, .09f);
        box(-0.09f,0.05f,0,    0.09f,0.10f,0.22f);
        box( 0.09f,0.05f,0,    0.09f,0.10f,0.22f);
        box(0,0.05f,-0.09f,    0.22f,0.10f,0.09f);
        box(0,0.05f, 0.09f,    0.22f,0.10f,0.09f);

        // Tronco
        glColor3f(.52f, .32f, .13f);
        box(0,0.5f,0,  0.26f,1.0f,0.26f);
        glColor3f(.48f,.28f,.10f);
        box(0,1.2f,0,  0.19f,0.6f,0.19f);
        glColor3f(.44f,.25f,.08f);
        box(0,1.75f,0, 0.13f,0.4f,0.13f);

        // Follaje 5 niveles
        glColor3f(.09f,.42f,.09f); pyr(0,1.3f,0, 1.9f,1.1f);
        glColor3f(.11f,.50f,.11f); pyr(0,2.1f,0, 1.5f,0.95f);
        glColor3f(.14f,.56f,.14f); pyr(0,2.75f,0,1.15f,0.88f);
        glColor3f(.17f,.61f,.17f); pyr(0,3.25f,0,0.82f,0.78f);
        glColor3f(.20f,.65f,.20f); pyr(0,3.7f,0, 0.52f,0.72f);

        glPopMatrix();
    }

    // ── Roca HD con rotacion aleatoria ────────────────────────────────────
    private void renderRock(float x, float y, float z, float s, float rot) {
        glPushMatrix();
        glTranslatef(x, y, z);
        glRotatef(rot, 0, 1, 0); // rotacion aleatoria
        glScalef(s, s, s);

        glColor3f(.60f,.56f,.52f); box(0,    0.12f,0,     1.0f,0.24f,0.85f);
        glColor3f(.54f,.50f,.46f); box(0,    0.40f,0,     0.82f,0.38f,0.72f);
        glColor3f(.50f,.46f,.42f); box(-0.1f,0.68f,0.04f, 0.58f,0.26f,0.48f);
        glColor3f(.52f,.48f,.44f); box( 0.22f,0.52f,-0.1f,0.32f,0.20f,0.28f);
        glColor3f(.46f,.42f,.38f); box(-0.26f,0.48f,0.1f, 0.28f,0.18f,0.26f);
        // Grieta
        glColor3f(.28f,.26f,.23f); box(0.04f,0.58f,0.36f, 0.06f,0.32f,0.03f);
        // Musgo
        glColor3f(.22f,.42f,.18f); box(-0.14f,0.78f,0.08f,0.22f,0.04f,0.18f);

        glPopMatrix();
    }

    // ── Casa HD con detalles en todos los lados ───────────────────────────
    private void renderHouse(float x, float y, float z, float s) {
        glPushMatrix();
        glTranslatef(x, y, z);
        glScalef(s, s, s);

        // Cimientos
        glColor3f(.58f,.52f,.46f); box(0,0.1f,0, 3.2f,0.2f,3.2f);

        // Franja piedra base
        glColor3f(.68f,.62f,.55f); box(0,0.3f,0, 3.0f,0.4f,3.0f);

        // Paredes
        glColor3f(.88f,.78f,.63f); box(0,1.1f,0, 3.0f,2.0f,3.0f);

        // Techo
        glColor3f(.62f,.20f,.10f); roof(0,2.1f,0, 3.4f,1.4f,3.4f);
        // Alero
        glColor3f(.52f,.16f,.06f); box(0,2.12f,0, 3.5f,0.08f,3.5f);

        // Chimenea
        glColor3f(.52f,.48f,.43f); box(0.7f,2.8f,0.5f, 0.4f,0.8f,0.4f);
        glColor3f(.38f,.33f,.28f); box(0.7f,3.25f,0.5f, 0.52f,0.08f,0.52f);

        // === Frente (Z-) ===
        // Puerta
        glColor3f(.36f,.20f,.06f); box(0,0.65f,-1.505f, 0.65f,1.30f,0.05f);
        // Marco puerta
        glColor3f(.26f,.14f,.04f);
        box(0,1.35f,-1.505f,   0.75f,0.10f,0.05f);
        box(-0.37f,0.65f,-1.505f, 0.05f,1.30f,0.05f);
        box( 0.37f,0.65f,-1.505f, 0.05f,1.30f,0.05f);
        // Escalon
        glColor3f(.62f,.58f,.50f); box(0,0.05f,-1.7f, 0.85f,0.10f,0.42f);
        // Ventanas frente
        glColor3f(.72f,.86f,.94f);
        box(-0.9f,1.15f,-1.505f, 0.55f,0.55f,0.05f);
        box( 0.9f,1.15f,-1.505f, 0.55f,0.55f,0.05f);
        glColor3f(.48f,.33f,.13f);
        box(-0.9f,1.15f,-1.506f, 0.05f,0.55f,0.06f);
        box(-0.9f,1.15f,-1.506f, 0.55f,0.05f,0.06f);
        box( 0.9f,1.15f,-1.506f, 0.05f,0.55f,0.06f);
        box( 0.9f,1.15f,-1.506f, 0.55f,0.05f,0.06f);

        // === Lado derecho (X+) ===
        glColor3f(.72f,.86f,.94f); box(1.505f,1.15f,0, 0.05f,0.55f,0.55f);
        glColor3f(.48f,.33f,.13f);
        box(1.506f,1.15f,0, 0.06f,0.05f,0.55f);
        box(1.506f,1.15f,0, 0.06f,0.55f,0.05f);

        // === Lado izquierdo (X-) ===
        glColor3f(.72f,.86f,.94f); box(-1.505f,1.15f,0, 0.05f,0.55f,0.55f);
        glColor3f(.48f,.33f,.13f);
        box(-1.506f,1.15f,0, 0.06f,0.05f,0.55f);
        box(-1.506f,1.15f,0, 0.06f,0.55f,0.05f);

        // === Atras (Z+) ===
        glColor3f(.72f,.86f,.94f); box(0,1.15f,1.505f, 0.55f,0.55f,0.05f);
        glColor3f(.48f,.33f,.13f);
        box(0,1.15f,1.506f, 0.05f,0.55f,0.06f);
        box(0,1.15f,1.506f, 0.55f,0.05f,0.06f);

        glPopMatrix();
    }

private void renderGrass(float x, float y, float z, float s, float rot) {
    glDisable(GL_CULL_FACE);
    glDisable(GL_LIGHTING);
    glPushMatrix();
    glTranslatef(x, y, z);
    glRotatef(rot, 0, 1, 0);
    glScalef(s, s, s);

    // Solo 2 quads cruzados en lugar de 9 triangulos
    glBegin(GL_TRIANGLES);

    // Quad 1 - frente/atras
    glColor3f(.10f, .52f, .10f);
    glVertex3f(-0.15f, 0f,   0f);
    glVertex3f( 0.15f, 0f,   0f);
    glVertex3f( 0.05f, 0.9f, 0f);

    glColor3f(.15f, .62f, .12f);
    glVertex3f(-0.15f, 0f,   0f);
    glVertex3f( 0.15f, 0f,   0f);
    glVertex3f(-0.05f, 0.9f, 0f);

    // Quad 2 - perpendicular
    glColor3f(.10f, .52f, .10f);
    glVertex3f(0f, 0f,   -0.15f);
    glVertex3f(0f, 0f,    0.15f);
    glVertex3f(0f, 0.9f,  0.05f);

    glColor3f(.15f, .62f, .12f);
    glVertex3f(0f, 0f,   -0.15f);
    glVertex3f(0f, 0f,    0.15f);
    glVertex3f(0f, 0.9f, -0.05f);

    glEnd();

    glPopMatrix();
    glEnable(GL_LIGHTING);
}

    private void renderGrassShadow(float x, float z, float s) {
        glDisable(GL_LIGHTING);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0f, 0f, 0f, 0.30f);

        float dirX = x - sunX, dirZ = z - sunZ;
        float dist = (float)Math.sqrt(dirX*dirX + dirZ*dirZ);
        if(dist == 0) dist = 1f;
        float dx = dirX/dist, dz = dirZ/dist;

        float len = s * 1.2f;
        float rw  = s * 0.25f;

        // Sombra como elipse alargada
        int seg = 12;
        glBegin(GL_TRIANGLE_STRIP);
        for (int i = 0; i <= seg; i++) {
            float a    = (float)(Math.PI * 2 * i / seg);
            float cosA = (float)Math.cos(a);
            float sinA = (float)Math.sin(a);
            glVertex3f(x + cosA * rw,          0.02f, z + sinA * rw * 0.5f);
            glVertex3f(x + dx*len + cosA*rw*0.3f, 0.02f, z + dz*len + sinA*rw*0.3f);
        }
        glEnd();

        glDisable(GL_BLEND);
        glEnable(GL_LIGHTING);
    }
    // ── Sombra casa ───────────────────────────────────────────────────────
    private void renderHouseShadow(float x, float z, float s) {
        glDisable(GL_LIGHTING);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0f,0f,0f,0.38f);

        float dirX = x - sunX, dirZ = z - sunZ;
        float dist = (float)Math.sqrt(dirX*dirX+dirZ*dirZ);
        if(dist==0) dist=1f;
        float dx=dirX/dist, dz=dirZ/dist;
        float len = 3f*s;
        float sx=dx*len, sz=dz*len;
        float hw=s*1.6f;

        glBegin(GL_POLYGON);
        glVertex3f(x-hw,0.02f,z-hw);
        glVertex3f(x+hw,0.02f,z-hw);
        glVertex3f(x+hw,0.02f,z+hw);
        glVertex3f(x+hw+sx,0.02f,z+hw+sz);
        glVertex3f(x+sx*1.6f,0.02f,z+sz*1.6f);
        glVertex3f(x-hw+sx,0.02f,z+hw+sz);
        glVertex3f(x-hw,0.02f,z+hw);
        glEnd();

        glDisable(GL_BLEND);
        glEnable(GL_LIGHTING);
    }

    // ── Sombra arbol simplificada ─────────────────────────────────────────
   private void renderTreeShadow(float x, float z, float s) {
    float largoTronco = 90.0f;
    float anchoTronco = 16.0f;
    float estirarSombra = 8f;

    glDisable(GL_LIGHTING);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glColor4f(0f, 0f, 0f, 0.42f);

    float dirX = x - sunX;
    float dirZ = z - sunZ;
    float sunDist = (float)Math.sqrt(dirX*dirX + dirZ*dirZ);
    if(sunDist == 0) sunDist = 1.0f;

    float dx = dirX / sunDist;
    float dz = dirZ / sunDist;
    float px = -dz;
    float pz =  dx;

    float factorX = (0.9f * s) / 80.0f;
    float factorY = (s * estirarSombra) / (300.0f + largoTronco);

    float xIzq = 80.0f - (anchoTronco / 2.0f);
    float xDer = 80.0f + (anchoTronco / 2.0f);

    // Tronco
    glBegin(GL_QUADS);
    drawShadowVertex(xIzq, 0,           x, z, dx, dz, px, pz, factorX, factorY);
    drawShadowVertex(xDer, 0,           x, z, dx, dz, px, pz, factorX, factorY);
    drawShadowVertex(xDer, largoTronco, x, z, dx, dz, px, pz, factorX, factorY);
    drawShadowVertex(xIzq, largoTronco, x, z, dx, dz, px, pz, factorX, factorY);
    glEnd();

    // Follaje
    glBegin(GL_QUADS);
    drawShadowVertex(0,   largoTronco+0,   x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(160, largoTronco+0,   x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(140, largoTronco+40,  x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(20,  largoTronco+40,  x,z,dx,dz,px,pz,factorX,factorY);

    drawShadowVertex(10,  largoTronco+40,  x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(150, largoTronco+40,  x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(125, largoTronco+90,  x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(35,  largoTronco+90,  x,z,dx,dz,px,pz,factorX,factorY);

    drawShadowVertex(20,  largoTronco+90,  x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(140, largoTronco+90,  x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(110, largoTronco+150, x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(50,  largoTronco+150, x,z,dx,dz,px,pz,factorX,factorY);

    drawShadowVertex(30,  largoTronco+150, x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(130, largoTronco+150, x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(95,  largoTronco+220, x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(65,  largoTronco+220, x,z,dx,dz,px,pz,factorX,factorY);
    glEnd();

    // Punta
    glBegin(GL_TRIANGLES);
    drawShadowVertex(45,  largoTronco+220, x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(115, largoTronco+220, x,z,dx,dz,px,pz,factorX,factorY);
    drawShadowVertex(80,  largoTronco+300, x,z,dx,dz,px,pz,factorX,factorY);
    glEnd();

    glDisable(GL_BLEND);
    glEnable(GL_LIGHTING);
}

private void drawShadowVertex(float rawX, float rawY, float cx, float cz,
                               float dx, float dz, float px, float pz,
                               float fx, float fy) {
    float localX = (rawX - 80.0f) * fx;
    float localY = rawY * fy;
    glVertex3f(cx + dx*localY + px*localX, 0.02f, cz + dz*localY + pz*localX);
}

    // ── Sombra roca ───────────────────────────────────────────────────────
 private void renderRockShadow(float x, float z, float s) {
    glDisable(GL_LIGHTING);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glColor4f(0f, 0f, 0f, 0.42f);

    float dirX = x - sunX;
    float dirZ = z - sunZ;
    float dist = (float)Math.sqrt(dirX*dirX + dirZ*dirZ);
    if(dist == 0) dist = 1.0f;

    float dx = dirX / dist;
    float dz = dirZ / dist;

    float longitudSombra = 0.45f * s;
    float sx = dx * longitudSombra;
    float sz = dz * longitudSombra;

    float r = s * 0.8f;

    glBegin(GL_POLYGON);
    glVertex3f(x - r,              0.02f, z);
    glVertex3f(x - r*0.5f + sx,   0.02f, z - r*0.7f + sz);
    glVertex3f(x + r*0.5f + sx,   0.02f, z - r*0.5f + sz);
    glVertex3f(x + r,              0.02f, z);
    glVertex3f(x + r*0.7f + sx,   0.02f, z + r*0.6f + sz);
    glVertex3f(x - r*0.3f + sx,   0.02f, z + r*0.8f + sz);
    glEnd();

    glDisable(GL_BLEND);
    glEnable(GL_LIGHTING);
}

    // ── Geometria base ────────────────────────────────────────────────────
    private void box(float cx,float cy,float cz,float w,float h,float d) {
        float hw=w/2,hh=h/2,hd=d/2;
        glBegin(GL_QUADS);
        glNormal3f(0,0,1);  glVertex3f(cx-hw,cy-hh,cz+hd); glVertex3f(cx+hw,cy-hh,cz+hd); glVertex3f(cx+hw,cy+hh,cz+hd); glVertex3f(cx-hw,cy+hh,cz+hd);
        glNormal3f(0,0,-1); glVertex3f(cx+hw,cy-hh,cz-hd); glVertex3f(cx-hw,cy-hh,cz-hd); glVertex3f(cx-hw,cy+hh,cz-hd); glVertex3f(cx+hw,cy+hh,cz-hd);
        glNormal3f(-1,0,0); glVertex3f(cx-hw,cy-hh,cz-hd); glVertex3f(cx-hw,cy-hh,cz+hd); glVertex3f(cx-hw,cy+hh,cz+hd); glVertex3f(cx-hw,cy+hh,cz-hd);
        glNormal3f(1,0,0);  glVertex3f(cx+hw,cy-hh,cz+hd); glVertex3f(cx+hw,cy-hh,cz-hd); glVertex3f(cx+hw,cy+hh,cz-hd); glVertex3f(cx+hw,cy+hh,cz+hd);
        glNormal3f(0,1,0);  glVertex3f(cx-hw,cy+hh,cz+hd); glVertex3f(cx+hw,cy+hh,cz+hd); glVertex3f(cx+hw,cy+hh,cz-hd); glVertex3f(cx-hw,cy+hh,cz-hd);
        glNormal3f(0,-1,0); glVertex3f(cx-hw,cy-hh,cz-hd); glVertex3f(cx+hw,cy-hh,cz-hd); glVertex3f(cx+hw,cy-hh,cz+hd); glVertex3f(cx-hw,cy-hh,cz+hd);
        glEnd();
    }

    private void pyr(float cx,float cy,float cz,float base,float height) {
        float hb=base/2f;
        glBegin(GL_TRIANGLES);
        glNormal3f(0,0.7f,0.7f);  glVertex3f(cx,cy+height,cz); glVertex3f(cx-hb,cy,cz+hb); glVertex3f(cx+hb,cy,cz+hb);
        glNormal3f(0,0.7f,-0.7f); glVertex3f(cx,cy+height,cz); glVertex3f(cx+hb,cy,cz-hb); glVertex3f(cx-hb,cy,cz-hb);
        glNormal3f(-0.7f,0.7f,0); glVertex3f(cx,cy+height,cz); glVertex3f(cx-hb,cy,cz-hb); glVertex3f(cx-hb,cy,cz+hb);
        glNormal3f(0.7f,0.7f,0);  glVertex3f(cx,cy+height,cz); glVertex3f(cx+hb,cy,cz+hb); glVertex3f(cx+hb,cy,cz-hb);
        glEnd();
        glBegin(GL_QUADS);
        glNormal3f(0,-1,0);
        glVertex3f(cx-hb,cy,cz-hb); glVertex3f(cx+hb,cy,cz-hb);
        glVertex3f(cx+hb,cy,cz+hb); glVertex3f(cx-hb,cy,cz+hb);
        glEnd();
    }

    private void roof(float cx,float cy,float cz,float w,float h,float d) {
        float hw=w/2,hd=d/2;
        glBegin(GL_TRIANGLES);
        glNormal3f(0,0,1);  glVertex3f(cx,cy+h,cz+hd); glVertex3f(cx-hw,cy,cz+hd); glVertex3f(cx+hw,cy,cz+hd);
        glNormal3f(0,0,-1); glVertex3f(cx,cy+h,cz-hd); glVertex3f(cx+hw,cy,cz-hd); glVertex3f(cx-hw,cy,cz-hd);
        glEnd();
        glBegin(GL_QUADS);
        glNormal3f(-0.7f,0.7f,0); glVertex3f(cx,cy+h,cz+hd); glVertex3f(cx,cy+h,cz-hd); glVertex3f(cx-hw,cy,cz-hd); glVertex3f(cx-hw,cy,cz+hd);
        glNormal3f(0.7f,0.7f,0);  glVertex3f(cx,cy+h,cz-hd); glVertex3f(cx,cy+h,cz+hd); glVertex3f(cx+hw,cy,cz+hd); glVertex3f(cx+hw,cy,cz-hd);
        glNormal3f(0,-1,0);
        glVertex3f(cx-hw,cy,cz-hd); glVertex3f(cx+hw,cy,cz-hd);
        glVertex3f(cx+hw,cy,cz+hd); glVertex3f(cx-hw,cy,cz+hd);
        glEnd();
    }
public float getRockHeightAt(float px, float pz) {
    for (float[] r : rocks) {
        float dx=px-r[0], dz=pz-r[2];
        float dist=(float)Math.sqrt(dx*dx+dz*dz);
        float radio = r[3]*0.5f;
        if(dist < radio) {
            return r[3]*0.85f;
        }
    }
    return 0f;
}
    
public boolean hayColision(float px, float py, float pz) {
    for (float[] t : trees) {
        float dx=px-t[0], dz=pz-t[2];
        if((float)Math.sqrt(dx*dx+dz*dz) < t[3]*0.3f+0.4f) return true;
    }
    for (float[] r : rocks) {
        float dx=px-r[0], dz=pz-r[2];
        float dist=(float)Math.sqrt(dx*dx+dz*dz);
        float radio     = r[3]*0.5f+0.4f;
        float rocaAltura = r[3]*0.85f;

        if(dist < radio) {
            // Si el jugador viene desde arriba (saltando) no hay colision horizontal
            if(py >= rocaAltura - 0.1f) continue;
            // Si viene caminando desde el lado, bloquear
            return true;
        }
    }
    for (float[] h : houses) {
        float hw=h[3]*1.6f, hd=h[3]*1.6f;
        if(px>h[0]-hw && px<h[0]+hw && pz>h[2]-hd && pz<h[2]+hd) return true;
    }
    return false;
}



    private float lr(float a,float b,float t){ return a+(b-a)*t; }
    
}