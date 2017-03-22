package com.example.opengl_es;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class GLDrawer 
{
	private static final String TAG = "[GLDrawer]";
	private final int FLOAT_SIZE_BYTES = 4;
    private final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
	private final float[] mTriangleVerticesData = {
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
    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];
    private int glProgram;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;
    private int idFBO = -1;
    private int idRBO = -1;
    private int width;
    private int height;
    private int unityTextureID = -1;
    private boolean isCanShowInfo = false;
    
    public GLDrawer(int w,int h,boolean isCanShowInfo)
    {
    	this.isCanShowInfo = isCanShowInfo;
    	this.width = w;
    	this.height = h;
    	mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);
        Matrix.setIdentityM(mSTMatrix, 0);
        initShaderProgram();
    }
    
    public void initUnity(int utid)
    {
    	this.unityTextureID = utid;
    	 Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
         for(int x = 0; x < width; x++)
        	 for (int y = 0; y < height; y++)
        		 bitmap.setPixel(x, y, Color.argb(155, 255, 50, 255));
         ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
         bitmap.copyPixelsToBuffer(buffer);
         bitmap.recycle();
         
         GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
         checkGlError("1");
         GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, unityTextureID);
         checkGlError("2");
         GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
         checkGlError("12");
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
         if(isCanShowInfo)Log.d(TAG, "初始化UnityID完成: " + unityTextureID);
    }
    
    
    public void draw(int stid)
    {
    	int[] testBuffer = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, testBuffer, 0);

        if(isCanShowInfo)Log.d(TAG, "DrawFrame binded = " + testBuffer[0] + " idFBO = " + idFBO);

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
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, stid);
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
    }
    
    private void setupBuffers()
    {
    	if(isCanShowInfo)Log.d(TAG, "setupBuffers");

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
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_RGBA4, width, height);
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
    
    private void initShaderProgram()
    {
    	if(isCanShowInfo)Log.d(TAG, "初始化ShaderProgram");
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
         if(isCanShowInfo)Log.d(TAG, "初始化ShaderProgram完成...");
    }
    
    private int loadShader(int shaderType, String source) 
    {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) 
        {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) 
            {
            	if(isCanShowInfo)
            	{
            		 Log.e(TAG, "解析shader出错 " + shaderType + ":");
            		 Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
            	}
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }
    
    private void checkFrameBufferStatus()
    {
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        checkGlError("glCheckFramebufferStatus");
        if(isCanShowInfo)
        {
        	 switch (status)
	        {
	            case GLES20.GL_FRAMEBUFFER_COMPLETE:
	                Log.d(TAG, "complete");
	                break;
	            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
	                Log.e(TAG, "incomplete attachment");
	                break;
	            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
	                Log.e(TAG, "incomplete missing attachment");
	                break;
	            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
	                Log.e(TAG, "incomplete dimensions");
	                break;
	            case GLES20.GL_FRAMEBUFFER_UNSUPPORTED:
	                Log.e(TAG, "framebuffer unsupported");
	                break;
	            default : Log.d(TAG, "default");
	        }
        }
    }
    private void checkGlError(String op) 
    {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) 
        {
        	if(isCanShowInfo)Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
    private void checkLocation(int location, String label) 
    {
        if (location < 0) 
        {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }
    
    public void dispose()
    {
    	GLES20.glDeleteProgram(glProgram);
    	//GLES20.glViewport(0, 0, width, height);
    }
}
