package xwang.cordova.vcloud.liveplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.netease.neliveplayer.NELivePlayer;
import __PACKAGE_NAME__.R;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

public class LivePlayerActivity extends Activity {
  private String mUrl;
  private String mTitle;

  private LivePlayerView mStreamingView;
  private FrameLayout mControlOverlay;
  private RelativeLayout mTopView;
  private RelativeLayout mBottomView;
  private Button mBackBtn;
  private TextView mTitleLabel;
  private ImageButton mSnapshotBtn;
  private ImageButton mListBtn;
  private Button mSendBtn;
  private EditText mInputText;
  private LinearLayout mBuffering;
  private static TextView mChannelText;

  private static Context mContext;
  private static CallbackContext pluginCallbackContext;

  private boolean isChannelHide = false;
  public static Handler UIHandler = new Handler(Looper.getMainLooper());

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

    mSnapshotBtn = (ImageButton) findViewById(R.id.snapshotBtn);
    mSnapshotBtn.setOnClickListener(mOnClickEvent);

    mListBtn = (ImageButton) findViewById(R.id.listBtn);
    mListBtn.setOnClickListener(mOnClickEvent);

    mSendBtn = (Button) findViewById(R.id.sendBtn);
    mSendBtn.setOnClickListener(mOnClickEvent);

    mChannelText = (TextView) findViewById(R.id.channelText);
    mChannelText.setMovementMethod(new ScrollingMovementMethod());
    mInputText = (EditText) findViewById(R.id.inputText);

    mBuffering = (LinearLayout) findViewById(R.id.buffering);
  }

  public static void addChannelMessage(final String name, final String message) {
    UIHandler.post(new Runnable() {
      @Override
      public void run() {
        Spannable spannable = new SpannableString(name + ": " + message + "\n");
        spannable.setSpan(new ForegroundColorSpan(Color.rgb(28,236,253)), 0, name.length(),  Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        mChannelText.append(spannable);

        final int scrollAmount = mChannelText.getLayout().getLineTop(mChannelText.getLineCount()) - mChannelText.getHeight();
        if (scrollAmount > 0)
          mChannelText.scrollTo(0, scrollAmount);
        else
          mChannelText.scrollTo(0, 0);
      }
    });
  }

  public static void setMessageCallbackContext(CallbackContext context) {
    pluginCallbackContext = context;
  }

  View.OnClickListener mOnClickEvent = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      if (view.getId() == R.id.backBtn) {
        onDestroy();
        finish();
      }
      else if (view.getId() == R.id.snapshotBtn) {
        mStreamingView.getSnapshot();
      }
      else if (view.getId() == R.id.listBtn) {
        isChannelHide = !isChannelHide;
        if (isChannelHide) {
          mChannelText.setVisibility(View.INVISIBLE);
        }
        else {
          mChannelText.setVisibility(View.VISIBLE);
        }
      }
      else if (view.getId() == R.id.sendBtn) {
        PluginResult result = new PluginResult(PluginResult.Status.OK, mInputText.getText().toString());
        result.setKeepCallback(true);
        if (pluginCallbackContext != null) {
          pluginCallbackContext.sendPluginResult(result);
        }
        mInputText.setText("");
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mInputText.getWindowToken(), 0);
      }
      else if (view.getId() == R.id.controlOverlay) {
        hide();
      }
    }
  };

  NELivePlayer.OnCompletionListener mOnCompletionListener = new NELivePlayer.OnCompletionListener() {
    @Override
    public void onCompletion(NELivePlayer neLivePlayer) {
      new AlertDialog.Builder(mContext)
        .setTitle("")
        .setMessage("直播已结束")
        .setPositiveButton("确定",
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
      AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
      builder.setTitle("");
      builder.setMessage("直播已结束");
      builder.setPositiveButton("确定",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            onDestroy();
            finish();
          }
        });
      builder.setCancelable(false);
      builder.show();
      return true;
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
    mControlOverlay.setVisibility(View.VISIBLE);
    mTopView.setVisibility(View.VISIBLE);
    mBottomView.setVisibility(View.VISIBLE);
    mChannelText.setVisibility(View.VISIBLE);
  }

  public void hide() {
    mControlOverlay.setVisibility(View.INVISIBLE);
    mTopView.setVisibility(View.INVISIBLE);
    mBottomView.setVisibility(View.INVISIBLE);
    mChannelText.setVisibility(View.INVISIBLE);
  }

  @Override
  public void onStart() {
    super.onStart();
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
