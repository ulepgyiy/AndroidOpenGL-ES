package com.example.opengl_es;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

public class AndroidVideoPlayer
{
	private static final String TAG = "[AndroidVideoPlayer]";
	// δ��ʼ��
	private static final int PLAY_STATUS_NONE = 0;
	// ���ڲ�����
	private static final int PLAY_STATUS_PLAYING = 1;
	// ������ͣ��
	private static final int PLAY_STATUS_PAUSE = 2;
	// �Ѿ�ֹͣ����
	private static final int PLAY_STATUS_STOPPED = 3;
	// ���Ŵ���
	private static final int PLAY_STATUS_ERROR = 4;
	// ���ڼ�����
	private static final int PLAY_STATUS_LOADING = 5;
	// ׼�����
	private static final int PLAY_STATUS_READY = 6;
	// ��Unity�д�������Texture2D����ID
	private int UnityTextureID = -1;
	// Android�в���Video����������ID
	private int VideoTextureID = -1;
	private Activity activity;
	private String videoPath = "";
	private MediaPlayer player;
	private SurfaceTexture texture;
	private Surface surface;
	
	private boolean isNeedUpdateFrame = false;
	private int videoWidth = 0;
	private int videoHeight = 0;
	private boolean isPrepared = false;
	private boolean isGetVideoSize = false;
	private boolean isCanShowInfo = false;
	private GLDrawer drawer;
	private boolean isCanPlay = false;
	private boolean isPause = false;
	
	private int currentStatus = PLAY_STATUS_NONE;
	
	public AndroidVideoPlayer(Activity ac,boolean isShowDebugInfo)
	{
		this.activity = ac;
		isCanShowInfo = isShowDebugInfo;
	}
	public AndroidVideoPlayer(Activity ac)
	{
		this(ac,false);
	}
	public void Play()
	{
		isCanPlay = true;
		isPause = false;
		controllVideo();
	}
	public void Stop()
	{
		isCanPlay = false;
		isPause = false;
		controllVideo();
	}
	public void Pause()
	{
		isPause = true;
		controllVideo();
	}
	
	public void Reset()
	{
		releasePlayer();
		releaseOpenGLES();
	}
	
	public void SetVideoPath(String path)
	{
		if(videoPath.equals(path))return;
		currentStatus = PLAY_STATUS_NONE;
		videoHeight = 0;
		videoWidth = 0;
		isGetVideoSize = false;
		isPrepared = false;
		videoPath = path;
		createPlayer();
	}
	public int SetTexID(int id)
	{
		int[] textureHandle;
		if(id==-1)
		{
			// Unity������IDҲ��Android�д���ȥ
			textureHandle = new int[2];
			// ��������id������һ�����ɶ����������������ȫ�����id 
			GLES20.glGenTextures(2, textureHandle, 0);
			id = textureHandle[1];
		}else
		{
			textureHandle = new int[1];
			// ��������id������һ�����ɶ����������������ȫ�����id 
			GLES20.glGenTextures(1, textureHandle, 0);
		}
		VideoTextureID = textureHandle[0];
		UnityTextureID = id;
		if(isCanShowInfo)Log.d(TAG,"--->��������ID��"+id+"   "+VideoTextureID);
		initOpenGLES();
		return UnityTextureID;
	}
	
	private void createPlayer()
	{
		releasePlayer();
		currentStatus = PLAY_STATUS_LOADING;
		if(videoPath==null||videoPath=="")return;
		if(isCanShowInfo)Log.d(TAG, "������Ƶ��ַ--->"+videoPath);
		activity.runOnUiThread(new Runnable() 
		{
			@Override
			public void run() 
			{
				player = new MediaPlayer();
				try 
				{
					player.setDataSource(videoPath);
					player.setOnInfoListener(onInfo);
					player.setOnErrorListener(onError);
					player.setOnPreparedListener(onPrepared);
					player.setOnVideoSizeChangedListener(onVideoSizeChange);
					if(surface!=null)player.setSurface(surface);
				} catch (Exception e) 
				{
					e.printStackTrace();
					return;
				} 
				player.prepareAsync();
			}
		});
	}
	MediaPlayer.OnInfoListener onInfo = new MediaPlayer.OnInfoListener() 
	{
		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) 
		{
			return false;
		}
	};
	MediaPlayer.OnErrorListener onError = new MediaPlayer.OnErrorListener() 
	{
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) 
		{
			currentStatus = PLAY_STATUS_ERROR;
			if(isCanShowInfo)Log.d(TAG, "��Ƶ���Ŵ���--->:"+what+"   "+extra);
			return false;
		}
	};
	MediaPlayer.OnVideoSizeChangedListener onVideoSizeChange = new MediaPlayer.OnVideoSizeChangedListener() 
	{
		@Override
		public void onVideoSizeChanged(MediaPlayer mp, int width, int height) 
		{
			videoWidth = mp.getVideoWidth();
			videoHeight = mp.getVideoHeight();
			isGetVideoSize = true;
			if(isCanShowInfo)Log.d(TAG, "��ȡ����Ƶ�ߴ�--->:"+videoWidth+"   "+videoHeight);
			autoPlayVideo();
		}
	};
	MediaPlayer.OnPreparedListener onPrepared = new MediaPlayer.OnPreparedListener() 
	{
		@Override
		public void onPrepared(MediaPlayer mp) 
		{
			videoWidth = mp.getVideoWidth();
			videoHeight = mp.getVideoHeight();
			isPrepared = true;
			if(isCanShowInfo)Log.d(TAG, "--->��Ƶ׼�����");
			autoPlayVideo();
		}
	};
	
	private void controllVideo()
	{
		if(!isGetVideoSize||!isPrepared)return;
		if(player==null||currentStatus==PLAY_STATUS_ERROR)return;
		if(isCanPlay)
		{
			if(isPause)
			{
				if(player.isPlaying())player.pause();
				currentStatus = PLAY_STATUS_PAUSE;
				if(isCanShowInfo)Log.d(TAG, "--->��Ƶ������ͣ��");
			}else
			{
				if(isCanShowInfo)Log.d(TAG, "--->׼��������Ƶ");
				if(!player.isPlaying())player.start();
				currentStatus = PLAY_STATUS_PLAYING;
				if(isCanShowInfo)Log.d(TAG, "--->��ʼ������Ƶ");
			}
		}else
		{
			if(player.isPlaying())player.stop();
			currentStatus = PLAY_STATUS_STOPPED;
			if(isCanShowInfo)Log.d(TAG, "--->δ���ò��ŷ���");
		}
	}
	private void autoPlayVideo()
	{
		if(!isGetVideoSize||!isPrepared)return;
		if(currentStatus!=PLAY_STATUS_ERROR)currentStatus = PLAY_STATUS_READY;
		controllVideo();
	}
	SurfaceTexture.OnFrameAvailableListener onFrameAvailable = new SurfaceTexture.OnFrameAvailableListener() 
	{
		@Override
		public void onFrameAvailable(SurfaceTexture surfaceTexture) 
		{
			if(isCanShowInfo)Log.d(TAG,"--->���֡����");
			isNeedUpdateFrame = true;
		}
	};
	////��ʼ��OpenGLES ��ز���
	private void initOpenGLES()
	{
		releaseOpenGLES();
		if(VideoTextureID==-1||UnityTextureID==-1)return;
		drawer = new GLDrawer(videoWidth, videoHeight,isCanShowInfo);
		drawer.initUnity(UnityTextureID);
		texture = new SurfaceTexture(VideoTextureID);
		texture.setDefaultBufferSize(videoWidth, videoHeight);
		texture.setOnFrameAvailableListener(onFrameAvailable);
		surface = new Surface(texture);
		if(player!=null)player.setSurface(surface);
		if(isCanShowInfo)Log.d(TAG,"--->��ʼ��ʼ��OpenGL-ES");
	}
	// ������Ƶ����
	public void UpdateVideoFrame()
	{
		if(isNeedUpdateFrame&&texture!=null&&drawer!=null)
		{
			synchronized(this)
			{
				if(isCanShowInfo)Log.d(TAG,"--->����֡����");
				isNeedUpdateFrame = false;
				texture.updateTexImage();
				float[] videoTextureTransform  = new float[16];
				texture.getTransformMatrix(videoTextureTransform);
				drawer.draw(VideoTextureID);
			}
		}
	}
	private void releasePlayer()
	{
		if(player!=null)
		{
			player.stop();
			player.reset();
			player.release();
			player = null;
		}
		videoHeight = 0;
		videoWidth = 0;
		isGetVideoSize = false;
		isPrepared = false;
	}
	private void releaseOpenGLES()
	{
		if(surface!=null)
		{
			surface.release();
			surface = null;
		}
		if(texture!=null)
		{
			texture.releaseTexImage();
			texture.release();
			texture = null;
		}
		if(drawer!=null)
		{
			drawer.dispose();
			drawer = null;
		}
	}
	
	public int GetPlayerStatus()
	{
		return currentStatus;
	}
	
	public void Dispose()
	{
		releasePlayer();
		releaseOpenGLES();
		activity = null;
	}
	public String GetVideoSize()
	{
		return videoWidth+":"+videoHeight;
	}
	public String GetCurrentVideoPath()
	{
		return videoPath;
	}
}
