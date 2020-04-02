package com.xt.vlc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import org.videolan.libvlc.MediaPlayer;

public class MainActivity extends AppCompatActivity {

    private VlcModel mVlcModel;
    private Handler  mHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HandlerThread handlerThread = new HandlerThread("");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        mVlcModel = new VlcModel("rtsp://192.168.1.254/xxxx.mov", (SurfaceView) findViewById(R.id.surfaceV), mEventListener);

        //Android6.0以下不能隐藏状态栏
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M)) {
            mVlcModel.setLandscapeDisplayHeight(mVlcModel.getLandscapeDisplayHeight() - getStatusBarHeight(this));
        }

        mVlcModel.updateVideoSurfaces();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mVlcModel.attachViews();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mVlcModel.detachViews();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mVlcModel.release();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mVlcModel.updateVideoSurfaces();
    }

    MediaPlayer.EventListener mEventListener = new MediaPlayer.EventListener() {
        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (event.type) {
                case MediaPlayer.Event.Opening:
                    break;
                case MediaPlayer.Event.Playing:
                    break;
                case MediaPlayer.Event.Buffering:
                    break;
                case MediaPlayer.Event.Paused:
                    break;
                case MediaPlayer.Event.Stopped:
                    break;
                case MediaPlayer.Event.PositionChanged:
                    break;
                case MediaPlayer.Event.TimeChanged:
                    break;
                case MediaPlayer.Event.SeekableChanged:
                    break;
                case MediaPlayer.Event.PausableChanged:
                    break;
                case MediaPlayer.Event.MediaChanged:
                    break;
                case MediaPlayer.Event.EndReached:
                    break;
                case MediaPlayer.Event.EncounteredError:
                    break;
                default:
                    break;
            }
        }
    };

    public void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT);
            }
        });
    }

    /**
     * 获取状态栏高度
     *
     * @param context context
     * @return 状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        // 获得状态栏高度
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return context.getResources().getDimensionPixelSize(resourceId);
    }
}
