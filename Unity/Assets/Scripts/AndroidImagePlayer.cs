using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System;

public class AndroidImagePlayer : MonoBehaviour 
{
	private AndroidJavaObject mainContext;
	private AndroidJavaObject player;
	private Texture2D texture;
	private bool useMobileID = false;
	private Boolean isNeedSetTexture = false;
	private Boolean isAndroid = false;
    // 默认读取Android SD卡上的目录，比如设置为 Images/bg.jpg  表示读取的路径为  /storage/emulated/0/Images/bg.jpg
    public string imagePath;

	void Start () 
	{
		isAndroid = Application.platform == RuntimePlatform.Android;
		if (!isAndroid)return;
		AndroidJavaClass ajc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		mainContext = ajc.GetStatic<AndroidJavaObject>("currentActivity");
        player = new AndroidJavaObject ("com.example.opengl_es.AndroidImagePlayer", mainContext);
    }
	void Update () 
	{
		if (!isAndroid)return;
        if (imagePath == null|| imagePath=="") return;
		if (texture == null) 
		{
            StartCoroutine(CopyStreamingAssetAndLoad(imagePath));
            //SetImagePath();
            CreateTexture ();
		} else 
		{
			if (isNeedSetTexture) {
				UpdateRendererTexture ();
			}
			GL.InvalidateState ();
		}

        //StartCoroutine();
	}
	private void UpdateRendererTexture()
	{
		if (texture == null)return;
		isNeedSetTexture = false;
		MeshRenderer renderer = GetComponent<MeshRenderer> ();
		if (renderer != null) 
		{
			if (renderer.material.mainTexture != texture)  
				renderer.material.mainTexture = texture;
		}
	}
    private void SetImagePath()
    {
        //string filePath = System.IO.Path.Combine(Application.streamingAssetsPath, imagePath);
        //string str = Application.streamingAssetsPath +"/"+ imagePath;
        Debug.Log("-->"+imagePath);
        player.Call("SetImagePath", imagePath);
    }


    private void CreateTexture()
	{
		if (useMobileID == false)
		{
			texture = new Texture2D (1024, 768, TextureFormat.ARGB32, false);
            player.Call<int> ("SetTexID",(int)texture.GetNativeTexturePtr());
		} else 
		{
			int tex = player.Call<int>("SetTexID",-1);
			texture = Texture2D.CreateExternalTexture (1024, 768, TextureFormat.ARGB32, false, false, new IntPtr(tex));
			//texture.UpdateExternalTexture ();
		}
		isNeedSetTexture = true;
	}


    IEnumerator CopyStreamingAssetAndLoad(string strURL)
    {
        strURL = strURL.Trim();
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
                imagePath = write_path;
                SetImagePath();
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
            imagePath = write_path;
            //imagePath = "file://" + write_path;
            SetImagePath();
        }
    }
}
