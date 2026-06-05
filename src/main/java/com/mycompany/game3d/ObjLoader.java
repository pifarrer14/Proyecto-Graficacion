package com.mycompany.game3d;

import java.io.*;
import java.util.*;
import static org.lwjgl.opengl.GL11.*;

public class ObjLoader {

    // Guardamos el modelo en memoria para no leerlo cada frame
    private static List<float[]> vertices = null;
    private static List<float[]> normals  = null;
    private static List<float[]> colors   = null;
    private static List<int[]>   faces    = null;
    private static String loadedFile      = null;

    public static void load(String filename) throws Exception {

        // Solo lee el archivo una vez
        if (!filename.equals(loadedFile)) {
            vertices   = new ArrayList<>();
            normals    = new ArrayList<>();
            colors     = new ArrayList<>();
            faces      = new ArrayList<>();
            loadedFile = filename;

            InputStream is = ObjLoader.class.getResourceAsStream("/" + filename);
            if (is == null) throw new Exception("No se encontro: " + filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            // Leer material
            Map<String, float[]> materials = new HashMap<>();
            float[] currentColor = {1f, 1f, 1f};
            String mtlFile = filename.replace(".obj", ".mtl");

            // Cargar colores del .mtl
            InputStream mis = ObjLoader.class.getResourceAsStream("/" + mtlFile);
            if (mis != null) {
                BufferedReader mbr = new BufferedReader(new InputStreamReader(mis));
                String mline;
                String currentMat = "";
                while ((mline = mbr.readLine()) != null) {
                    mline = mline.trim();
                    if (mline.startsWith("newmtl ")) {
                        currentMat = mline.substring(7).trim();
                        materials.put(currentMat, new float[]{1f, 1f, 1f});
                    } else if (mline.startsWith("Kd ") && !currentMat.isEmpty()) {
                        String[] p = mline.split("\\s+");
                        materials.put(currentMat, new float[]{
                            Float.parseFloat(p[1]),
                            Float.parseFloat(p[2]),
                            Float.parseFloat(p[3])
                        });
                    }
                }
                mbr.close();
            }

            // Leer geometria del .obj
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("v ")) {
                    String[] p = line.split("\\s+");
                    vertices.add(new float[]{
                        Float.parseFloat(p[1]),
                        Float.parseFloat(p[2]),
                        Float.parseFloat(p[3])
                    });

                } else if (line.startsWith("vn ")) {
                    String[] p = line.split("\\s+");
                    normals.add(new float[]{
                        Float.parseFloat(p[1]),
                        Float.parseFloat(p[2]),
                        Float.parseFloat(p[3])
                    });

                } else if (line.startsWith("usemtl ")) {
                    String matName = line.substring(7).trim();
                    currentColor = materials.getOrDefault(matName, new float[]{1f,1f,1f});

                } else if (line.startsWith("f ")) {
                    String[] p = line.split("\\s+");
                    List<int[]> faceVerts = new ArrayList<>();
                    for (int i = 1; i < p.length; i++) {
                        String[] idx = p[i].split("/");
                        int vi = Integer.parseInt(idx[0]) - 1;
                        int ni = (idx.length > 2 && !idx[2].isEmpty())
                                 ? Integer.parseInt(idx[2]) - 1 : -1;
                        faceVerts.add(new int[]{vi, ni});
                        colors.add(currentColor.clone());
                    }
                    // Triangular si es quad (4 vertices)
                    if (faceVerts.size() == 4) {
                        faces.add(faceVerts.get(0));
                        faces.add(faceVerts.get(1));
                        faces.add(faceVerts.get(2));
                        faces.add(faceVerts.get(0));
                        faces.add(faceVerts.get(2));
                        faces.add(faceVerts.get(3));
                    } else {
                        for (int[] fv : faceVerts) faces.add(fv);
                    }
                }
            }
            br.close();
            System.out.println("Modelo cargado: " + vertices.size() + " vertices, " + faces.size()/3 + " triangulos");
        }

        // Dibujar el modelo
        glBegin(GL_TRIANGLES);
        for (int i = 0; i + 2 < faces.size(); i += 3) {
            for (int j = 0; j < 3; j++) {
                int idx = i + j;
                int[] f = faces.get(idx);

                // Color del material
                if (idx < colors.size()) {
                    float[] c = colors.get(idx);
                    glColor3f(c[0], c[1], c[2]);
                }

                // Normal
                if (f[1] >= 0 && f[1] < normals.size()) {
                    float[] n = normals.get(f[1]);
                    glNormal3f(n[0], n[1], n[2]);
                }

                // Vertice
                float[] v = vertices.get(f[0]);
                glVertex3f(v[0], v[1], v[2]);
            }
        }
        glEnd();
    }
}