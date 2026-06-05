package com.mycompany.game3d;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import java.nio.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {
    public static final int WIDTH  = 1280;
    public static final int HEIGHT = 720;
    public static final String TITLE = "3D World";

    private long window;
    private GameWorld world;
    private Player    player;
    private Camera    camera;
    private InputHandler input;

    public void run() { init(); loop(); cleanup(); }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("No se pudo inicializar GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE,   GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_DEPTH_BITS, 24);

        window = glfwCreateWindow(WIDTH, HEIGHT, TITLE, NULL, NULL);
        if (window == NULL) throw new RuntimeException("No se pudo crear la ventana");

        try (MemoryStack stack = stackPush()) {
            IntBuffer pw = stack.mallocInt(1), ph = stack.mallocInt(1);
            glfwGetWindowSize(window, pw, ph);
            GLFWVidMode vm = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwSetWindowPos(window, (vm.width()-pw.get(0))/2, (vm.height()-ph.get(0))/2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        // Base
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.4f, 0.6f, 0.9f, 1.0f);

        // SIN cull face para que sombras y sol se vean bien
        glDisable(GL_CULL_FACE);

        // Blend para transparencias y sombras
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Iluminacion
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
        glShadeModel(GL_SMOOTH);

        // Luz ambiental
        FloatBuffer amb = BufferUtils.createFloatBuffer(4);
        amb.put(new float[]{0.35f, 0.35f, 0.35f, 1.0f}).flip();
        glLightfv(GL_LIGHT0, GL_AMBIENT, amb);

        // Luz difusa (sol)
        FloatBuffer dif = BufferUtils.createFloatBuffer(4);
        dif.put(new float[]{1.0f, 0.95f, 0.8f, 1.0f}).flip();
        glLightfv(GL_LIGHT0, GL_DIFFUSE, dif);

        // Especular
        FloatBuffer spe = BufferUtils.createFloatBuffer(4);
        spe.put(new float[]{0.4f, 0.4f, 0.4f, 1.0f}).flip();
        glLightfv(GL_LIGHT0, GL_SPECULAR, spe);

        // Capturar mouse
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        // Proyeccion
        glViewport(0, 0, WIDTH, HEIGHT);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float fov=70f, aspect=(float)WIDTH/HEIGHT, near=0.1f, far=500f;
        float ys=(float)(1.0/Math.tan(Math.toRadians(fov/2)));
        float xs=ys/aspect, fl=far-near;
        FloatBuffer proj = BufferUtils.createFloatBuffer(16);
        proj.put(new float[]{
            xs,0,0,0,
            0,ys,0,0,
            0,0,-((far+near)/fl),-1,
            0,0,-((2*near*far)/fl),0
        }).flip();
        glLoadMatrixf(proj);
        glMatrixMode(GL_MODELVIEW);

        input  = new InputHandler(window);
        player = new Player(0, 0f, 0);
        world  = new GameWorld();
        camera = new Camera(player, world);
        System.out.println("WASD=mover | Mouse=camara | ESC=salir");
    }

    private void loop() {
        double last = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now  = glfwGetTime();
            float  dt   = (float)(now - last);
            last = now;

            glfwPollEvents();
            if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
                glfwSetWindowShouldClose(window, true);

            input.update();
            world.update(dt);
            player.update(dt, input, world, camera);
            camera.update(dt, input);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glLoadIdentity();
            camera.applyView();

            // Posicion de la luz del sol (direccional)
            FloatBuffer lightPos = BufferUtils.createFloatBuffer(4);
            lightPos.put(new float[]{-1.5f, 0.8f, 0.3f, 0.0f}).flip();
            glLightfv(GL_LIGHT0, GL_POSITION, lightPos);

          
            // Renderizar en orden correcto
            world.render(player.x, player.z);
            player.renderShadow(world.getSunX(), world.getSunZ());
            player.render();
            glfwSetWindowTitle(window, TITLE + "  FPS: " + (int)(1f/dt));
            glfwSwapBuffers(window);
        }
    }

    private void cleanup() {
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public static void main(String[] args) { new Main().run(); }
}