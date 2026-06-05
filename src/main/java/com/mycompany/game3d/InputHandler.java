package com.mycompany.game3d;

import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {
    private final long window;
    private double lastX=-1, lastY=-1, mouseDX, mouseDY, scrollY;
    private float cameraYaw;

    public InputHandler(long window) {
        this.window = window;
        glfwSetScrollCallback(window, (win, xo, yo) -> scrollY += yo);
    }

    public void update() {
        double[] mx=new double[1], my=new double[1];
        glfwGetCursorPos(window, mx, my);
        if (lastX<0) { lastX=mx[0]; lastY=my[0]; }
        mouseDX=mx[0]-lastX; mouseDY=my[0]-lastY;
        lastX=mx[0]; lastY=my[0];
        cameraYaw += (float)(mouseDX*0.15f);
    }

    public boolean isKeyDown(int key) { return glfwGetKey(window,key)==GLFW_PRESS; }
    public double getMouseDX()  { return mouseDX; }
    public double getMouseDY()  { return mouseDY; }
    public float  getCameraYaw(){ return cameraYaw; }
    public double getScrollY()  { double s=scrollY; scrollY=0; return s; }
}