package xwang.cordova.vcloud.liveplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.netease.neliveplayer.NELivePlayer;
import com.netease.neliveplayer.NEMediaPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

public class LivePlayerView extends SurfaceView {
  private static Context mContext;
  private int mVideoWidth;
  private int mVideoHeight;
  private int mSurfaceWidth;
  private int mSurfaceHeight;
  private SurfaceHolder mSurfaceHolder;
  private NELivePlayer mMediaPlayer;
  private boolean mIsPrepared;
  private String mUrl;

  private NELivePlayer.OnCompletionListener mOnCompletionListener = null;
  private NELivePlayer.OnErrorListener mOnErrorListener = null;
  private NELivePlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = null;
  private NELivePlayer.OnInfoListener mOnInfoListener = null;
  private View.OnTouchListener mOnTouchListener = null;

  private final static String TAG = "xwang.liveplayer";

  public LivePlayerView(Context context) {
    super(context);
    mContext = context;
    initView();
  }

  public LivePlayerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    mContext = context;
    initView();
  }

  public LivePlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mContext = context;
    initView();
  }

  private void initView() {
    mVideoWidth = 0;
    mVideoHeight = 0;
    getHolder().addCallback(mSHCallback);
    setFocusable(true);
    setFocusableInTouchMode(true);
    requestFocus();
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  public void setVideoScalingMode() {
    ViewGroup.LayoutParams layPara = getLayoutParams();
    int winWidth  = 0;
    int winHeight = 0;
    Rect rect = new Rect();
    this.getWindowVisibleDisplayFrame(rect);
    WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      DisplayMetrics metrics = new DisplayMetrics();
      display.getRealMetrics(metrics);
      winWidth  = metrics.widthPixels;
      winHeight = metrics.heightPixels - rect.top;
    }
    else {
      try {
        Method mRawWidth  = Display.class.getMethod("getRawWidth");
        Method mRawHeight = Display.class.getMethod("getRawHeight");
        winWidth  = (Integer) mRawWidth.invoke(display);
        winHeight = (Integer) mRawHeight.invoke(display) - rect.top;
      } catch (NoSuchMethodException e) {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        winWidth  = dm.widthPixels;
        winHeight = dm.heightPixels - rect.top;
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }

    float winRatio = (float) winWidth / winHeight;
    if (mVideoWidth > 0 && mVideoHeight > 0) {
      float aspectRatio = (float) (mVideoWidth) / mVideoHeight;
      mSurfaceHeight = mVideoHeight;
      mSurfaceWidth = mVideoWidth;

      if (winRatio < aspectRatio) {
        layPara.width  = winWidth;
        layPara.height = (int)(winWidth / aspectRatio);
      }
      else {
        layPara.width  = (int)(aspectRatio * winHeight);
        layPara.height = winHeight;
      }
      setLayoutParams(layPara);
      getHolder().setFixedSize(mSurfaceWidth, mSurfaceHeight);
//      Log.d(TAG, "Video: width = " + mVideoWidth + ", height = " + mVideoHeight);
//      Log.d(TAG, "Surface: width = " + mSurfaceWidth + ", height = " + mSurfaceHeight);
//      Log.d(TAG, "Window:width = " + winWidth + ", height = " + winHeight);
//      Log.d(TAG, "LayoutParams:width = " + layPara.width + ", height = " + layPara.height);
    }
  }


  SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
      mSurfaceHolder = surfaceHolder;
      openVideo();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int w, int h) {
      mSurfaceHolder = surfaceHolder;
      mSurfaceWidth = w;
      mSurfaceHeight = h;
      if (mMediaPlayer != null) {
        mMediaPlayer.setDisplay(surfaceHolder);
      }
      start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
      mSurfaceHolder = null;
      if (mMediaPlayer != null) {
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;
      }
    }
  };

  public void setVideoPath(String path) {
    mUrl = path;
  }

  private void openVideo() {
    if (mUrl == null || mSurfaceHolder == null) {
      return;
    }
    Intent intent = new Intent("com.android.music.musicservicecommand");
    intent.putExtra("command", "pause");
    mContext.sendBroadcast(intent);

    if (mMediaPlayer != null) {
      mMediaPlayer.reset();
      mMediaPlayer.release();
      mMediaPlayer = null;
    }
    try {
      mIsPrepared = false;
      mMediaPlayer = new NEMediaPlayer();
      mMediaPlayer.setBufferStrategy(0);
      mMediaPlayer.setHardwareDecoder(false);
      mMediaPlayer.setOnPreparedListener(mPreparedListener);
      mMediaPlayer.setOnCompletionListener(mCompletionListener);
      mMediaPlayer.setOnErrorListener(mErrorListener);
      mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
      mMediaPlayer.setOnInfoListener(mInfoListener);

      int ret = mMediaPlayer.setDataSource(mUrl);
      if (ret < 0) {
        if (mMediaPlayer != null) {
          mMediaPlayer.reset();
          mMediaPlayer.release();
          mMediaPlayer = null;
        }
        return;
      }
      mMediaPlayer.setDisplay(mSurfaceHolder);
      mMediaPlayer.setScreenOnWhilePlaying(true);
      mMediaPlayer.prepareAsync(mContext);
    }
    catch (IOException e) {
      mErrorListener.onError(mMediaPlayer, -1, 0);
      return;
    }
    catch (IllegalArgumentException e) {
      mErrorListener.onError(mMediaPlayer, -1, 0);
      return;
    }
  }

  NELivePlayer.OnPreparedListener mPreparedListener = new NELivePlayer.OnPreparedListener() {
    @Override
    public void onPrepared(NELivePlayer mediaPlayer) {
      mVideoWidth = mediaPlayer.getVideoWidth();
      mVideoHeight = mediaPlayer.getVideoHeight();
      if (mVideoWidth != 0 && mVideoHeight != 0) {
        setVideoScalingMode();
      }
      mIsPrepared = true;
      start();
    }
  };

  public void setOnCompletionListener(NELivePlayer.OnCompletionListener l) {
    mOnCompletionListener = l;
  }

  NELivePlayer.OnCompletionListener mCompletionListener = new NELivePlayer.OnCompletionListener() {
    @Override
    public void onCompletion(NELivePlayer neLivePlayer) {
      if (mOnCompletionListener != null) {
        mOnCompletionListener.onCompletion(neLivePlayer);
      }
    }
  };

  public void setOnErrorListener(NELivePlayer.OnErrorListener l) {
    mOnErrorListener = l;
  }

  NELivePlayer.OnErrorListener mErrorListener = new NELivePlayer.OnErrorListener() {
    @Override
    public boolean onError(NELivePlayer neLivePlayer, int i, int i1) {
      if (mOnErrorListener != null) {
        return mOnErrorListener.onError(neLivePlayer, i, i1);
      }
      return false;
    }
  };

  public void setOnBufferingUpdateListener(NELivePlayer.OnBufferingUpdateListener l) {
    mOnBufferingUpdateListener = l;
  }

  NELivePlayer.OnBufferingUpdateListener mBufferingUpdateListener = new NELivePlayer.OnBufferingUpdateListener() {
    @Override
    public void onBufferingUpdate(NELivePlayer neLivePlayer, int i) {
      if (mOnBufferingUpdateListener != null) {
        mOnBufferingUpdateListener.onBufferingUpdate(neLivePlayer, i);
      }
    }
  };

  public void setOnInfoListener(NELivePlayer.OnInfoListener l) {
    mOnInfoListener = l;
  }

  NELivePlayer.OnInfoListener mInfoListener = new NELivePlayer.OnInfoListener() {
    @Override
    public boolean onInfo(NELivePlayer neLivePlayer, int i, int i1) {
      if (mOnInfoListener != null) {
        mOnInfoListener.onInfo(neLivePlayer, i, i1);
      }
      return false;
    }
  };


  public void setOnTouchListener(View.OnTouchListener l) {
    mOnTouchListener = l;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (mOnTouchListener != null) {
      return mOnTouchListener.onTouch(this, ev);
    }
    return true;
  }

  public void start() {
    if (mMediaPlayer != null && mIsPrepared) {
      mMediaPlayer.start();
    }
  }

  public void setMute(boolean mute) {
    if (mMediaPlayer != null && mIsPrepared) {
      mMediaPlayer.setMute(mute);
    }
  }

  public void getSnapshot() {
    if (mMediaPlayer != null && mIsPrepared) {
      Bitmap bitmap = Bitmap.createBitmap(mVideoWidth, mVideoHeight, Bitmap.Config.ARGB_8888);
      mMediaPlayer.getSnapshot(bitmap);
      File imageFile = savePhoto(bitmap);
      // Update image gallery
      if (imageFile != null) {
        scanPhoto(imageFile);
        Toast.makeText(mContext, "截图成功", Toast.LENGTH_SHORT).show();
      }
      else {
        Toast.makeText(mContext, "截图失败", Toast.LENGTH_SHORT).show();
      }
    }
  }

  private File savePhoto(Bitmap bmp) {
    File retVal = null;

    try {
      Calendar c = Calendar.getInstance();
      String date = "" + c.get(Calendar.DAY_OF_MONTH)
        + c.get(Calendar.MONTH)
        + c.get(Calendar.YEAR)
        + c.get(Calendar.HOUR_OF_DAY)
        + c.get(Calendar.MINUTE)
        + c.get(Calendar.SECOND);

      String deviceVersion = Build.VERSION.RELEASE;
      int check = deviceVersion.compareTo("2.3.3");

      File folder;
      /*
       * File path = Environment.getExternalStoragePublicDirectory(
       * Environment.DIRECTORY_PICTURES ); //this throws error in Android
       * 2.2
       */
      if (check >= 1) {
        folder = Environment
          .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        if(!folder.exists()) {
          folder.mkdirs();
        }
      } else {
        folder = Environment.getExternalStorageDirectory();
      }

      File imageFile = new File(folder, "snapshot_" + date.toString() + ".jpg");

      FileOutputStream out = new FileOutputStream(imageFile);
      bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
      out.flush();
      out.close();

      retVal = imageFile;
    } catch (Exception e) {
    }
    return retVal;
  }

  /* Invoke the system's media scanner to add your photo to the Media Provider's database,
   * making it available in the Android Gallery application and to other apps. */
  private void scanPhoto(File imageFile)
  {
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    Uri contentUri = Uri.fromFile(imageFile);
    mediaScanIntent.setData(contentUri);
    mContext.sendBroadcast(mediaScanIntent);
  }
}
