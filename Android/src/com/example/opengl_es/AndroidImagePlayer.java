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
			Log.d(TAG,"�޷���ȡ��Bitmap");
		}
		rendererImage();
	}
	
	
	public int SetTexID(int id)
	{
		if(id==-1)
		{
			int[] textureHandle = new int[1];
			// ��������id������һ�����ɶ����������������ȫ�����id 
			GLES20.glGenTextures(1, textureHandle, 0);
			id = textureHandle[0];
		}
		UnityTextureID = id;
		Log.d(TAG,"--->����ID��"+id);
		rendererImage();
		return UnityTextureID;
	}
	
	private void rendererImage()
	{
		if(bitmap==null||UnityTextureID==-1)return;
		Log.d(TAG,"--->����ͼƬ��������	ID��"+UnityTextureID);
		// ����������������id��Ϊ������ÿ��bind֮�󣬺��������������Ǹ����� 
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, UnityTextureID);
		//ָ�������ʽ�����������������������ظ���ʽ 
		//GL_TEXTURE_WRAP_S 
		//GL_TEXTURE_WRAP_T 
		//�����ڷŴ����С��ͬ��������Զ�������ʱ�Ĵ�������������Ҫ��Ϊ�˱���ͬһ��������ʹ��ʱ��Զ�����������Ƚ��������� 
		//GL_TEXTURE_MAG_FILTER 
		//GL_TEXTURE_MIN_FILTER
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		//��������ͼ������
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
	}
	
}
