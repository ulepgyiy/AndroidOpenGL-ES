using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System;

public class AndroidVideoPlayer : MonoBehaviour
{

    // 未初始化
    private static int PLAY_STATUS_NONE = 0;
    // 正在播放中
    private static int PLAY_STATUS_PLAYING = 1;
    // 处于暂停中
    private static int PLAY_STATUS_PAUSE = 2;
    // 已经停止播放
    private static int PLAY_STATUS_STOPPED = 3;
    // 播放错误
    private static int PLAY_STATUS_ERROR = 4;
    // 正在加载中
    private static int PLAY_STATUS_LOADING = 5;
    // 准备完成
    private static int PLAY_STATUS_READY = 6;


    private AndroidJavaObject mainContext;
    private AndroidJavaObject player;
    private Texture2D texture;
    private bool useMobileID = false;
    private bool isNeedSetTexture = false;
    private bool isAndroid = false;
    private int videoWidth = 0;
    private int videoHeight = 0;
    private bool isInitVideoPath = false;
    private bool isApplicationPause = false;
    private bool isApplicationFocus = false;

    public string videoPath;
    public bool isAutoPlay = false;
    public bool isDebug = false;
    void Start ()
    {
        isAndroid = Application.platform == RuntimePlatform.Android;
        if (!isAndroid) return;
        AndroidJavaClass ajc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
        mainContext = ajc.GetStatic<AndroidJavaObject>("currentActivity");
        player = new AndroidJavaObject("com.example.opengl_es.AndroidVideoPlayer", mainContext, isDebug);
    }
	
	void Update ()
    {
        if (!isAndroid) return;
        if (videoPath == null || videoPath == "") return;
        // 先设置视频播放地址
        // 然后获取视频的大小
        // 再设置Unity的播放纹理ID
        // 更新纹理数据
        if(texture==null)
        {
            if(isInitVideoPath==false)
            {
                StartCoroutine(CopyStreamingAssetAndLoad(videoPath));
                isInitVideoPath = true;
            }
            
            //SetVideoPath();
            if (videoWidth == 0 || videoHeight == 0)
            {
                GetVideoSize();
            }else
            {
                CreateTexture();
                if(isAutoPlay)
                {
                    Play();
                }
            }
        }else
        {
            if (isNeedSetTexture)
            {
                UpdateRendererTexture();
            }
            int status = GetPlayerStatus();
            /// 正在播放视频
            if(status==PLAY_STATUS_PLAYING)
            {
                UpdateVideoFrame();
                GL.InvalidateState();
            }
        }
    }
    void OnEnable()
    {
        isApplicationFocus = false;
        isApplicationPause = false;
    }
    void OnApplicationPause()
    {
        isApplicationPause = !isApplicationPause;
        OnApplicationHandler();
    }
    void OnApplicationFocus()
    {
        isApplicationFocus = !isApplicationFocus;
        OnApplicationHandler();
    }

    private void OnApplicationHandler()
    {
        if (isApplicationFocus&&isApplicationPause)
        {
            //Debug.Log("--->激活到前台");
            OnApplicationStart();
        }else if(!isApplicationPause&&!isApplicationFocus)
        {
            //Debug.Log("--->切换到后台");
            OnApplicationStop();
        }
    }

    private bool isNeedRecoverPlay = false;
    private void OnApplicationStart()
    {
        if (isNeedRecoverPlay) Play();
        isNeedRecoverPlay = false;
    }
    private void OnApplicationStop()
    {
        int currentStatus = GetPlayerStatus();
        if (currentStatus == PLAY_STATUS_PLAYING)
        {
            Pause();
            isNeedRecoverPlay = true;
        }
    }
    public void Play()
    {
        if(player!=null)
            player.Call("Play");
    }

    public void Stop()
    {
        if (player != null)
            player.Call("Stop");
    }

    public void Pause()
    {
        if (player != null)
            player.Call("Pause");
    }

    private int GetPlayerStatus()
    {
        return player.Call<int>("GetPlayerStatus");
    }

    private void GetVideoSize()
    {
        string size = player.Call<string>("GetVideoSize");
        if (size.Equals("0:0")) return;
        string[] point = size.Split(':');
        videoWidth = int.Parse(point[0]);
        videoHeight = int.Parse(point[1]);
    }

    private void UpdateVideoFrame()
    {
        player.Call("UpdateVideoFrame");
    }

    private void SetVideoPath()
    {
        player.Call("SetVideoPath", videoPath);
    }
    private void CreateTexture()
    {
        if (videoWidth == 0 || videoHeight == 0) return;
        if (useMobileID == false)
        {
            texture = new Texture2D(videoWidth, videoHeight, TextureFormat.ARGB32, false);
            player.Call<int>("SetTexID", (int)texture.GetNativeTexturePtr());
        }
        else
        {
            int tex = player.Call<int>("SetTexID", -1);
            texture = Texture2D.CreateExternalTexture(videoWidth, videoHeight, TextureFormat.ARGB32, false, false, new IntPtr(tex));
        }
        isNeedSetTexture = true;
    }
    private void UpdateRendererTexture()
    {
        if (texture == null) return;
        isNeedSetTexture = false;
        MeshRenderer renderer = GetComponent<MeshRenderer>();
        if (renderer != null)
        {
            if (renderer.material.mainTexture != texture)
                renderer.material.mainTexture = texture;
        }
    }

    IEnumerator CopyStreamingAssetAndLoad(string strURL)
    {
        strURL = strURL.Trim();
        if (strURL.IndexOf("//:") == -1)
        {
            string write_path = Application.persistentDataPath + "/" + strURL;
            if (System.IO.File.Exists(write_path) == false)
            {
                Debug.Log("CopyStreamingAssetAndLoad : " + strURL);
                WWW www = new WWW(Application.streamingAssetsPath + "/" + strURL);
                yield return www;
                if (string.IsNullOrEmpty(www.error))
                {
                    Debug.Log(write_path);
                    System.IO.File.WriteAllBytes(write_path, www.bytes);
                    //imagePath = "file://" + write_path;
                    videoPath = write_path;
                    SetVideoPath();
                }
                else
                {
                    Debug.Log(www.error);
                }
                www.Dispose();
                www = null;
            }
            else
            {
                videoPath = write_path;
                //imagePath = "file://" + write_path;
                SetVideoPath();
            }
        }
        else
        {
            videoPath = strURL;
            SetVideoPath();
        }
    }
}
