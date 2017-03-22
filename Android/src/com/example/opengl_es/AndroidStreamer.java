package com.example.opengl_es;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

public class AndroidStreamer 
{
	private final int FLOAT_SIZE_BYTES = 4;
    private final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

    private Activity _currActivity;
    private MediaPlayer _streamConnection;

    private Surface _cachedSurface;
    private SurfaceTexture _cachedSurfaceTexture;

    private Boolean isNewFrame = false;

    //open gl
    private int texWidth = 320;
    private int texHeight = 240;
    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];
    private int glProgram;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;
    private int unityTextureID = -1;
    private int mTextureId = -1;   //surface texture id
    private int idFBO = -1;
    private int idRBO = -1;
    
    private int idx = 150;
    
    private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f,  1.0f, 0, 0.f, 1.f,
            1.0f,  1.0f, 0, 1.f, 1.f,
    };
    private FloatBuffer mTriangleVertices;
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";
    private final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +      // highp here doesn't seem to matter
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    public AndroidStreamer(Activity ac) 
    {
        Log.d("Unity", "AndroidStreamer was initialized");
        _currActivity = ac;
        mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);
        Matrix.setIdentityM(mSTMatrix, 0);

        initShaderProgram();
    }

    private void initShaderProgram()
    {
        Log.d("Unity", "initShaderProgram");
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        glProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(glProgram, vertexShader);
        checkGlError("glAttachVertexShader");
        GLES20.glAttachShader(glProgram, fragmentShader);
        checkGlError("glAttachFragmentShader");
        GLES20.glLinkProgram(glProgram);
        checkGlError("glLinkProgram");

        maPositionHandle = GLES20.glGetAttribLocation(glProgram, "aPosition");
        checkLocation(maPositionHandle, "aPosition");
        maTextureHandle = GLES20.glGetAttribLocation(glProgram, "aTextureCoord");
        checkLocation(maTextureHandle, "aTextureCoord");

        muMVPMatrixHandle = GLES20.glGetUniformLocation(glProgram, "uMVPMatrix");
        checkLocation(muMVPMatrixHandle, "uVMPMatrix");
        muSTMatrixHandle = GLES20.glGetUniformLocation(glProgram, "uSTMatrix");
        checkLocation(muSTMatrixHandle, "uSTMatrix");
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("Unity", "Could not compile shader " + shaderType + ":");
                Log.e("Unity", GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("Unity", op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    private void checkFrameBufferStatus()
    {
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        checkGlError("glCheckFramebufferStatus");
        switch (status)
        {
            case GLES20.GL_FRAMEBUFFER_COMPLETE:
                Log.d("Unity", "complete");
                break;
            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                Log.e("Unity", "incomplete attachment");
                break;
            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                Log.e("Unity", "incomplete missing attachment");
                break;
            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                Log.e("Unity", "incomplete dimensions");
                break;
            case GLES20.GL_FRAMEBUFFER_UNSUPPORTED:
                Log.e("Unity", "framebuffer unsupported");
                break;

            default : Log.d("Unity", "default");
        }
    }

    private void initGLTexture()
    {
        Log.d("Unity", "initGLTexture");
        int textures[] = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures initGLTexture");
        mTextureId = textures[0];

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        checkGlError("glActiveTexture initGLTexture");
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
        checkGlError("glBindTexture initGLTexture");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        checkGlError("glTexParameterf initGLTexture");
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        checkGlError("glTexParameterf initGLTexture");
    }
    
    public int GetTexturePtr(int id)
    {
        Bitmap bitmap = Bitmap.createBitmap(texWidth, texHeight, Bitmap.Config.ARGB_8888);
        for(int x = 0; x < texWidth; x++)
        {
            for (int y = 0; y < texHeight; y++)
            {
                bitmap.setPixel(x, y, Color.argb(155, 255, 50, 255));
            }
        }
        Log.d("Unity", "Bitmap is: " + bitmap);

        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(buffer);

        //GLES20.glEnable(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        //checkGlError("glEnable GetTexturePtr");

//        int textures[] = new int[1];
//        GLES20.glGenTextures(1, textures, 0);
//        checkGlError("0");
//        unityTextureID = textures[0];
        unityTextureID = id;
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        checkGlError("1");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, unityTextureID);
        checkGlError("2");

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texWidth, texHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        checkGlError("12");
        //GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        //checkGlError("3");
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        checkGlError("4");
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        checkGlError("5");
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("6");
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("7");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        checkGlError("8");

        setupBuffers();
        Log.d("Unity", "texture id returned: " + unityTextureID);

        return unityTextureID;
    }

    private void setupBuffers()
    {
        Log.d("Unity", "setupBuffers");

        //framebuffer
        int buffers[] = new int[1];
        GLES20.glGenFramebuffers(1, buffers, 0);
        checkGlError("9");
        idFBO = buffers[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, idFBO);
        checkGlError("10");

        //render buffer
        int rbuffers[] = new int[1];
        GLES20.glGenRenderbuffers(1, rbuffers, 0);
        checkGlError("glGenRenderBuffers setupBuffers");
        idRBO = rbuffers[0];
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, idRBO);
        checkGlError("glBindRenderBuffer setupBuffers");
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_RGBA4, texWidth, texHeight);
        checkGlError("glRenderBufferStorage setupBuffers");

        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER, idRBO);
        checkGlError("glFramebufferRenderbuffer setupBuffers");

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, unityTextureID, 0);
        checkGlError("glFrameBufferTexture2D");

        checkFrameBufferStatus();

        GLES20.glClearColor(1.0f, 0.5f, 0.0f, 1.0f);
        checkGlError("glClearColor setupBuffers");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        checkGlError("glClear setupBuffers");
    }

    public void DrawFrame()
    {
        if(isNewFrame && mSTMatrix != null) 
        {
        	idx--;
        	if(idx>0)return;
        	synchronized (this) 
            {
        		_cachedSurfaceTexture.updateTexImage();
                mSTMatrix = new float[16];
                _cachedSurfaceTexture.getTransformMatrix(mSTMatrix);
            }
            int[] testBuffer = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, testBuffer, 0);

            Log.d("Unity", "DrawFrame binded = " + testBuffer[0] + " idFBO = " + idFBO);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, idFBO);
            checkGlError("glBindFrameBuffer DrawFrame");

            GLES20.glClearColor(0.0f, 1.0f, 0.2f, 1.0f);
            checkGlError("glClearColor DrawFrame");
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            checkGlError("glClear DrawFrame");

            GLES20.glUseProgram(glProgram);
            checkGlError("glUseProgram DrawFrame");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            checkGlError("glActiveTexture DrawFrame");
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
            checkGlError("glBindTexture DrawFrame");

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer DrawFrame");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            checkGlError("glEnableVertexAttribArray DrawFrame");

            Matrix.setIdentityM(mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            checkGlError("glUniformMatrix4fv MVP onFrameAvailable");
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
            checkGlError("glUniformMatrix4fv ST onFrameAvailable");

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays onFrameAvailable");

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            checkGlError("glBindFrameBuffer 0 onFrameAvailable");
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
            checkGlError("glBindTexture onFrameAvailable");

            isNewFrame = false;
        }
    }

    public void LaunchStream(String streamLink) 
    {
        final String path = streamLink; //"http://dlqncdn.miaopai.com/stream/MVaux41A4lkuWloBbGUGaQ__.mp4"; //"rtmp://live.hkstv.hk.lxdns.com/live/hks";
        Log.i("Unity", "hop hop1 = " + path);
        _currActivity.runOnUiThread(new Runnable() 
        {
            @Override
            public void run() 
            {
            	try
            	{
            		 _streamConnection = new MediaPlayer();
                     _streamConnection.setDataSource(path);
                     _streamConnection.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                         @Override
                         public boolean onError(MediaPlayer mp, int what, int extra) {
                             Log.i("Unity", "some error, I don't know. what = " + what + " extra = " + extra);
                             return false;
                         }
                     });
                     _streamConnection.setOnPreparedListener(new MediaPlayer.OnPreparedListener() 
                     {
                         @Override
                         public void onPrepared(MediaPlayer mediaPlayer) 
                         {
                             Log.i("Unity", "hop hop5");
                             mediaPlayer.start();
                         }
                     });
                     initGLTexture();
                     _cachedSurfaceTexture = new SurfaceTexture(mTextureId);
                     _cachedSurfaceTexture.setDefaultBufferSize(texWidth, texHeight);
                     _cachedSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() 
                     {
                         @Override
                         public void onFrameAvailable(SurfaceTexture surfaceTexture) 
                         {
                        	 isNewFrame = true;
//                             synchronized (this) 
//                             {
//                                 surfaceTexture.updateTexImage();
//
//                                 mSTMatrix = new float[16];
//                                 surfaceTexture.getTransformMatrix(mSTMatrix);
//                                 isNewFrame = true;
//                             }
                         }
                     });

                     _cachedSurface = new Surface(_cachedSurfaceTexture);
                     _streamConnection.setSurface(_cachedSurface);
                     _streamConnection.prepareAsync();
                     //_streamConnection.setSurfaceToPlayer(_cachedSurface);
                     Log.i("Unity", "You're the best around!");
            	}catch(Exception e)
            	{
            		e.printStackTrace();
            	}
            }
        });
    }
}
