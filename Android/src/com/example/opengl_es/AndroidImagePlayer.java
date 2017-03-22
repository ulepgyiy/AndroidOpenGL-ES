package com.example.opengl_es;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Environment;
import android.util.Log;

public class AndroidImagePlayer 
{
	private static final String TAG = "AndroidImagePlayer";
	private int UnityTextureID = -1;
	private Activity activity;
	private Bitmap bitmap;
	public AndroidImagePlayer(Activity ac)
	{
		this.activity = ac;
	}
	public void SetImagePath(String path)
	{
		SetImagePath(path,false);
	}
	public void SetImagePath(String path,boolean isSDFile)
	{
		//bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getPath()+"/Images/test.jpg");//BitmapFactory.decodeResource(activity.getResources(), R.drawable.bg);
		if(bitmap!=null)
		{
			bitmap.recycle();
			bitmap = null;
		}
		if(isSDFile)
		{
			bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getPath()+"/"+path);
		}else
		{
			bitmap = BitmapFactory.decodeFile(path);
//			AssetManager assetManager = activity.getAssets();
//			try 
//			{
//				InputStream stream = assetManager.open(path);
//				bitmap = BitmapFactory.decodeStream(stream);
//			} catch (IOException e) 
//			{
//				e.printStackTrace();
//			}
		}
		if(bitmap==null)
		{
			Log.d(TAG,"无法获取到Bitmap");
		}
		rendererImage();
	}
	
	
	public int SetTexID(int id)
	{
		if(id==-1)
		{
			int[] textureHandle = new int[1];
			// 生成纹理id，可以一次生成多个，后续操作纹理全靠这个id 
			GLES20.glGenTextures(1, textureHandle, 0);
			id = textureHandle[0];
		}
		UnityTextureID = id;
		Log.d(TAG,"--->纹理ID："+id);
		rendererImage();
		return UnityTextureID;
	}
	
	private void rendererImage()
	{
		if(bitmap==null||UnityTextureID==-1)return;
		Log.d(TAG,"--->绘制图片到纹理中	ID："+UnityTextureID);
		// 操作纹理，传入纹理id作为参数，每次bind之后，后续操作的纹理都是该纹理 
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, UnityTextureID);
		//指定纹理格式。这里包括纹理横向和纵向的重复方式 
		//GL_TEXTURE_WRAP_S 
		//GL_TEXTURE_WRAP_T 
		//纹理在放大和缩小（同样纹理离远和离近）时的处理，这种设置主要是为了避免同一个纹理反复使用时，远处的纹理反而比近处的清晰 
		//GL_TEXTURE_MAG_FILTER 
		//GL_TEXTURE_MIN_FILTER
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		//给纹理传入图像数据
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
	}
	
}
