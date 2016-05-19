package xwang.cordova.vcloud.liveplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.netease.neliveplayer.NELivePlayer;
import com.xinfu.uuke.local.R;

public class LivePlayerActivity extends Activity {
  private String mUrl;
  private String mTitle;

  private LivePlayerView mStreamingView;
  private FrameLayout mControlOverlay;
  private RelativeLayout mTopView;
  private RelativeLayout mBottomView;
  private Button mBackBtn;
  private TextView mTitleLabel;
  private ImageButton mMuteBtn;
  private ImageButton mSnapshotBtn;
  private SeekBar mVolumeSlider;
  private AudioManager mAudioManager;
  private LinearLayout mBuffering;

  private boolean isHide = false;
  private boolean isMute = false;

  private static Context mContext;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    mContext = this;
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_liveplayer);

    mUrl = getIntent().getStringExtra("url");
    mTitle = getIntent().getStringExtra("title");

    mStreamingView = (LivePlayerView) findViewById(R.id.streamingView);
    mStreamingView.setVideoPath(mUrl);
    mStreamingView.setOnCompletionListener(mOnCompletionListener);
    mStreamingView.setOnErrorListener(mOnErrorListener);
    mStreamingView.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
    mStreamingView.setOnInfoListener(mOnInfoListener);
    mStreamingView.setOnTouchListener(mOnTouchListener);
    mStreamingView.requestFocus();
    mStreamingView.start();

    mControlOverlay = (FrameLayout) findViewById(R.id.controlOverlay);
    mControlOverlay.setOnClickListener(mOnClickEvent);

    mTopView = (RelativeLayout) findViewById(R.id.topView);

    mBackBtn = (Button) findViewById(R.id.backBtn);
    mBackBtn.setOnClickListener(mOnClickEvent);

    mTitleLabel = (TextView) findViewById(R.id.titleLabel);
    mTitleLabel.setText(mTitle);

    mBottomView = (RelativeLayout) findViewById(R.id.bottomView);

    mMuteBtn = (ImageButton) findViewById(R.id.muteBtn);
    mMuteBtn.setOnClickListener(mOnClickEvent);

    mVolumeSlider = (SeekBar) findViewById(R.id.volumeSlider);
    mVolumeSlider.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    mVolumeSlider.setMax(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
    mVolumeSlider.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

    mSnapshotBtn = (ImageButton) findViewById(R.id.snapshotBtn);
    mSnapshotBtn.setOnClickListener(mOnClickEvent);

    mBuffering = (LinearLayout) findViewById(R.id.buffering);
  }

  View.OnClickListener mOnClickEvent = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      if (view.getId() == R.id.backBtn) {
        onDestroy();
        finish();
      }
      else if (view.getId() == R.id.muteBtn) {
        isMute = !isMute;
        if (isMute) {
          mMuteBtn.setImageResource(R.drawable.mute);
          mStreamingView.setMute(true);
        }
        else {
          mMuteBtn.setImageResource(R.drawable.volume);
          mStreamingView.setMute(false);
        }
      }
      else if (view.getId() == R.id.snapshotBtn) {
        mStreamingView.getSnapshot();
      }
      else if (view.getId() == R.id.controlOverlay) {
        hide();
      }
    }
  };

  SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
      mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
  };

  NELivePlayer.OnCompletionListener mOnCompletionListener = new NELivePlayer.OnCompletionListener() {
    @Override
    public void onCompletion(NELivePlayer neLivePlayer) {
      new AlertDialog.Builder(mContext)
        .setTitle("")
        .setMessage("直播结束")
        .setPositiveButton("OK",
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              onDestroy();
              finish();
            }
          })
        .setCancelable(false)
        .show();
    }
  };

  NELivePlayer.OnErrorListener mOnErrorListener = new NELivePlayer.OnErrorListener() {
    @Override
    public boolean onError(final NELivePlayer neLivePlayer, int i, int i1) {
      new AlertDialog.Builder(mContext)
        .setTitle("")
        .setMessage("直播尚未开始")
        .setPositiveButton("Ok",
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              onDestroy();
              finish();
            }
          })
        .setCancelable(false)
        .show();
      return false;
    }
  };

  NELivePlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new NELivePlayer.OnBufferingUpdateListener() {
    @Override
    public void onBufferingUpdate(NELivePlayer neLivePlayer, int i) {

    }
  };

  NELivePlayer.OnInfoListener mOnInfoListener = new NELivePlayer.OnInfoListener() {
    @Override
    public boolean onInfo(NELivePlayer neLivePlayer, int what, int extra) {
      if (what == NELivePlayer.NELP_BUFFERING_START) {
        mBuffering.setVisibility(View.VISIBLE);
      }
      else if (what == NELivePlayer.NELP_BUFFERING_END) {
        mBuffering.setVisibility(View.INVISIBLE);
      }
      return true;
    }
  };

  View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
      show();
      return true;
    }
  };

  public void show() {
    isHide = false;
    mControlOverlay.setVisibility(View.VISIBLE);
    mTopView.setVisibility(View.VISIBLE);
    mBottomView.setVisibility(View.VISIBLE);
  }

  public void hide() {
    isHide = true;
    mControlOverlay.setVisibility(View.INVISIBLE);
    mTopView.setVisibility(View.INVISIBLE);
    mBottomView.setVisibility(View.INVISIBLE);
  }


  @Override
  public void onStart() {
    super.onStart();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == event.KEYCODE_VOLUME_DOWN || keyCode == event.KEYCODE_VOLUME_UP) {
      mVolumeSlider.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public void onRestart() {
    super.onRestart();
  }

  @Override
  public void onResume() {
    super.onResume();
  }
}