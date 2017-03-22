using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class AndroidStreamer : MonoBehaviour {

    private AndroidJavaObject mainContext;
    private AndroidJavaObject player;
    private Texture2D texture;
    private bool useMobileID = false;
    private bool isNeedSetTexture = false;
    private bool isAndroid = false;

    public string videoPath;
    void Start()
    {
        isAndroid = Application.platform == RuntimePlatform.Android;
        if (!isAndroid) return;
        AndroidJavaClass ajc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
        mainContext = ajc.GetStatic<AndroidJavaObject>("currentActivity");
        player = new AndroidJavaObject("com.example.opengl_es.AndroidStreamer", mainContext);
    }

    void Update()
    {
        if (!isAndroid) return;
        if (videoPath == null || videoPath == "") return;
        if (texture == null)
        {
            
            CreateTexture();
            SetVideoPath();
        }
        else
        {
            if (isNeedSetTexture)
            {
                UpdateRendererTexture();
            }
            UpdateVideoFrame();
            GL.InvalidateState();
        }
    }

    private void UpdateVideoFrame()
    {
        player.Call("DrawFrame");
    }

    private void SetVideoPath()
    {
        //Debug.Log("-->" + videoPath);
        player.Call("LaunchStream", videoPath);
    }
    private void CreateTexture()
    {
        texture = new Texture2D(320, 240, TextureFormat.ARGB32, false);
        player.Call<int>("GetTexturePtr", (int)texture.GetNativeTexturePtr());

        /*int texPtr = player.Call<int>("GetTexturePtr");
        texture = Texture2D.CreateExternalTexture(320, 240, TextureFormat.RGBA32, false, false, new System.IntPtr(texPtr));*/
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
}
