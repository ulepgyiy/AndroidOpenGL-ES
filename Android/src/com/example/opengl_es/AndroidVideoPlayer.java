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
	// 未初始化
	private static final int PLAY_STATUS_NONE = 0;
	// 正在播放中
	private static final int PLAY_STATUS_PLAYING = 1;
	// 处于暂停中
	private static final int PLAY_STATUS_PAUSE = 2;
	// 已经停止播放
	private static final int PLAY_STATUS_STOPPED = 3;
	// 播放错误
	private static final int PLAY_STATUS_ERROR = 4;
	// 正在加载中
	private static final int PLAY_STATUS_LOADING = 5;
	// 准备完成
	private static final int PLAY_STATUS_READY = 6;
	// 从Unity中传过来的Texture2D纹理ID
	private int UnityTextureID = -1;
	// Android中播放Video创建的纹理ID
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
			// Unity的纹理ID也从Android中传过去
			textureHandle = new int[2];
			// 生成纹理id，可以一次生成多个，后续操作纹理全靠这个id 
			GLES20.glGenTextures(2, textureHandle, 0);
			id = textureHandle[1];
		}else
		{
			textureHandle = new int[1];
			// 生成纹理id，可以一次生成多个，后续操作纹理全靠这个id 
			GLES20.glGenTextures(1, textureHandle, 0);
		}
		VideoTextureID = textureHandle[0];
		UnityTextureID = id;
		if(isCanShowInfo)Log.d(TAG,"--->设置纹理ID："+id+"   "+VideoTextureID);
		initOpenGLES();
		return UnityTextureID;
	}
	
	private void createPlayer()
	{
		releasePlayer();
		currentStatus = PLAY_STATUS_LOADING;
		if(videoPath==null||videoPath=="")return;
		if(isCanShowInfo)Log.d(TAG, "加载视频地址--->"+videoPath);
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
			if(isCanShowInfo)Log.d(TAG, "视频播放错误--->:"+what+"   "+extra);
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
			if(isCanShowInfo)Log.d(TAG, "获取到视频尺寸--->:"+videoWidth+"   "+videoHeight);
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
			if(isCanShowInfo)Log.d(TAG, "--->视频准备完成");
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
				if(isCanShowInfo)Log.d(TAG, "--->视频处于暂停中");
			}else
			{
				if(isCanShowInfo)Log.d(TAG, "--->准备播放视频");
				if(!player.isPlaying())player.start();
				currentStatus = PLAY_STATUS_PLAYING;
				if(isCanShowInfo)Log.d(TAG, "--->开始播放视频");
			}
		}else
		{
			if(player.isPlaying())player.stop();
			currentStatus = PLAY_STATUS_STOPPED;
			if(isCanShowInfo)Log.d(TAG, "--->未调用播放方法");
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
			if(isCanShowInfo)Log.d(TAG,"--->获得帧数据");
			isNeedUpdateFrame = true;
		}
	};
	////初始化OpenGLES 相关参数
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
		if(isCanShowInfo)Log.d(TAG,"--->开始初始化OpenGL-ES");
	}
	// 更新视频数据
	public void UpdateVideoFrame()
	{
		if(isNeedUpdateFrame&&texture!=null&&drawer!=null)
		{
			synchronized(this)
			{
				if(isCanShowInfo)Log.d(TAG,"--->更新帧数据");
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
