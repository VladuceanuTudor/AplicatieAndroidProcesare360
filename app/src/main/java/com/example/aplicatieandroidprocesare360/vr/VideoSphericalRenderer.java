package com.example.aplicatieandroidprocesare360.vr;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL ES 2.0 renderer for 360° video using GL_TEXTURE_EXTERNAL_OES.
 * Video frames arrive via SurfaceTexture (fed by ExoPlayer).
 */
public class VideoSphericalRenderer implements android.opengl.GLSurfaceView.Renderer {

    // language=GLSL
    private static final String VERTEX_SHADER =
        "uniform mat4 uMVPMatrix;\n" +
        "uniform mat4 uSTMatrix;\n" +
        "attribute vec4 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "  gl_Position = uMVPMatrix * aPosition;\n" +
        "  vTexCoord = (uSTMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;\n" +
        "}\n";

    // language=GLSL — requires EGL image external extension for video textures
    private static final String FRAGMENT_SHADER =
        "#extension GL_OES_EGL_image_external : require\n" +
        "precision mediump float;\n" +
        "uniform samplerExternalOES uTexture;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "  gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
        "}\n";

    private static final int STACKS = 32;
    private static final int SLICES = 64;

    public interface OnSurfaceTextureReadyListener {
        void onSurfaceTextureReady(SurfaceTexture surfaceTexture);
    }

    private int program;
    private final int[] textureIds = new int[1];
    private SurfaceTexture surfaceTexture;
    private volatile boolean newFrameAvailable = false;

    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private ShortBuffer indexBuffer;
    private int indexCount;

    private final float[] rotationMatrix = new float[16];
    private final float[] projMatrix     = new float[16];
    private final float[] viewMatrix     = new float[16];
    private final float[] mvpMatrix      = new float[16];
    private final float[] stMatrix       = new float[16];

    private int screenWidth  = 1;
    private int screenHeight = 1;
    private boolean cardboardMode = false;
    private float   ipd           = 0.065f;

    private OnSurfaceTextureReadyListener listener;
    private android.app.Activity activity;

    public VideoSphericalRenderer(android.app.Activity activity,
                                  OnSurfaceTextureReadyListener listener) {
        this.activity = activity;
        this.listener = listener;
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.setIdentityM(stMatrix, 0);
        buildSphere();
    }

    public void setRotationMatrix(float[] m) {
        System.arraycopy(m, 0, rotationMatrix, 0, 16);
    }

    public void setCardboardMode(boolean enabled) { this.cardboardMode = enabled; }
    public void setIpd(float ipdMeters)           { this.ipd = ipdMeters; }

    public void onNewFrame() { newFrameAvailable = true; }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER);

        GLES20.glGenTextures(1, textureIds, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureIds[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        surfaceTexture = new SurfaceTexture(textureIds[0]);
        surfaceTexture.setOnFrameAvailableListener(st -> newFrameAvailable = true);

        if (listener != null && activity != null) {
            activity.runOnUiThread(() -> listener.onSurfaceTextureReady(surfaceTexture));
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        screenWidth  = width;
        screenHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (newFrameAvailable && surfaceTexture != null) {
                surfaceTexture.updateTexImage();
                surfaceTexture.getTransformMatrix(stMatrix);
                newFrameAvailable = false;
            }
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (cardboardMode) {
            drawEye(0, 0, screenWidth / 2, screenHeight, -ipd / 2f);
            drawEye(screenWidth / 2, 0, screenWidth / 2, screenHeight, ipd / 2f);
        } else {
            drawEye(0, 0, screenWidth, screenHeight, 0f);
        }
    }

    private void drawEye(int x, int y, int w, int h, float eyeOffset) {
        GLES20.glViewport(x, y, w, h);

        float aspect = (float) w / h;
        Matrix.perspectiveM(projMatrix, 0, 90f, aspect, 0.1f, 10f);

        float[] eyeTrans = new float[16];
        Matrix.setIdentityM(eyeTrans, 0);
        Matrix.translateM(eyeTrans, 0, eyeOffset, 0, 0);

        float[] combined = new float[16];
        Matrix.multiplyMM(combined, 0, eyeTrans, 0, rotationMatrix, 0);
        Matrix.invertM(viewMatrix, 0, combined, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, viewMatrix, 0);

        GLES20.glUseProgram(program);

        int uMVP      = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        int uSTMatrix = GLES20.glGetUniformLocation(program, "uSTMatrix");
        int uTexture  = GLES20.glGetUniformLocation(program, "uTexture");
        int aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        int aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord");

        GLES20.glUniformMatrix4fv(uMVP,      1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMatrix, 1, false, stMatrix,  0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureIds[0]);
        GLES20.glUniform1i(uTexture, 0);

        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoord);
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aTexCoord);
    }

    private void buildSphere() {
        int vertCount = (STACKS + 1) * (SLICES + 1);
        float[] vertices  = new float[vertCount * 3];
        float[] texCoords = new float[vertCount * 2];
        int vi = 0, ti = 0;

        for (int stack = 0; stack <= STACKS; stack++) {
            float phi = (float)(Math.PI * stack / STACKS);
            for (int slice = 0; slice <= SLICES; slice++) {
                float theta = (float)(2 * Math.PI * slice / SLICES);
                vertices[vi++] = (float)(Math.sin(phi) * Math.cos(theta));
                vertices[vi++] = (float)(Math.cos(phi));
                vertices[vi++] = (float)(Math.sin(phi) * Math.sin(theta));
                texCoords[ti++] = 1f - (float) slice / SLICES;
                texCoords[ti++] = (float) stack / STACKS;
            }
        }

        int[] indices = new int[STACKS * SLICES * 6];
        int idx = 0;
        for (int stack = 0; stack < STACKS; stack++) {
            for (int slice = 0; slice < SLICES; slice++) {
                int tl = stack * (SLICES + 1) + slice;
                int tr = tl + 1;
                int bl = tl + (SLICES + 1);
                int br = bl + 1;
                indices[idx++] = tl; indices[idx++] = bl; indices[idx++] = tr;
                indices[idx++] = tr; indices[idx++] = bl; indices[idx++] = br;
            }
        }

        vertexBuffer  = toFloatBuffer(vertices);
        texCoordBuffer = toFloatBuffer(texCoords);

        short[] shortIdx = new short[indices.length];
        for (int i = 0; i < indices.length; i++) shortIdx[i] = (short) indices[i];
        indexCount = shortIdx.length;
        indexBuffer = ByteBuffer.allocateDirect(shortIdx.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        indexBuffer.put(shortIdx).position(0);
    }

    private static FloatBuffer toFloatBuffer(float[] arr) {
        FloatBuffer buf = ByteBuffer.allocateDirect(arr.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(arr).position(0);
        return buf;
    }

    private static int buildProgram(String vert, String frag) {
        int v = compileShader(GLES20.GL_VERTEX_SHADER,   vert);
        int f = compileShader(GLES20.GL_FRAGMENT_SHADER, frag);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);
        return p;
    }

    private static int compileShader(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        return s;
    }

    public void release() {
        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
    }
}
