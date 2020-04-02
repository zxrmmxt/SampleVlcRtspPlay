package com.xt.vlc;

import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;

/**
 * created by XuTi on 2019/5/20 14:00
 */
public class VlcModel {
    private static final String TAG = VlcModel.class.getSimpleName();

    private LibVLC      mLibVlc;
    private MediaPlayer mMediaPlayer;

    private String mUriString;

    private MediaPlayer.EventListener mEventListener;

    private SurfaceView mSurfaceView = null;

    private int mPortraitDisplayWidth;
    private int mPortraitDisplayHeight;
    private int mLandscapeDisplayWidth;
    private int mLandscapeDisplayHeight;

    public VlcModel(String uriString, SurfaceView surfaceView, MediaPlayer.EventListener eventListener) {
        mUriString = uriString;
        mSurfaceView = surfaceView;

        mEventListener = eventListener;

        {
            Context        context       = mSurfaceView.getContext();
            WindowManager  windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm            = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(dm);

            int screenWidth  = dm.widthPixels;
            int screenHeight = dm.heightPixels;

            int portraitDisplayWidth;
            int portraitDisplayHeight;
            int landscapeDisplayWidth;
            int landscapeDisplayHeight;

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                portraitDisplayWidth = screenWidth;
                landscapeDisplayHeight = screenWidth;
                landscapeDisplayWidth = (int) (landscapeDisplayHeight * 16 / 9f);
            } else {
                portraitDisplayWidth = screenHeight;
                landscapeDisplayHeight = screenHeight;
                landscapeDisplayWidth = (int) (landscapeDisplayHeight * 16 / 9f);
            }
            portraitDisplayHeight = (int) (portraitDisplayWidth * 9 / 16f);

            mPortraitDisplayWidth = portraitDisplayWidth;
            mPortraitDisplayHeight = portraitDisplayHeight;
            mLandscapeDisplayWidth = landscapeDisplayWidth;
            mLandscapeDisplayHeight = landscapeDisplayHeight;
        }
    }

    private void init() {
        {
            final ArrayList<String> options = new ArrayList<>();
            options.add("-vvv");
            //RTSP帧缓冲大小，默认大小为100000
//            options.add(":rtsp-frame-buffer-size=5000");

//            options.add("--rtsp-caching=100");

            mLibVlc = new LibVLC(mSurfaceView.getContext(), options);

            mMediaPlayer = new MediaPlayer(mLibVlc);
        }

        {
            final Media media = new Media(mLibVlc, Uri.parse(mUriString));
            media.setHWDecoderEnabled(true, true);

//            media.addOption(":clock-jitter=0");
//            media.addOption(":clock-synchro=0");

            //文件缓存
//            media.addOption(":file-caching=1000");
            //网络缓存
            media.addOption(":network-caching=220");

            //直播缓存0
//            media.addOption(":live-caching=1000");
            //输出缓存
//            media.addOption(":sout-mux-caching=1000");

//            media.addOption(":codec=mediacodec,iomx,all");

            mMediaPlayer.setMedia(media);

            MyThreadUtils.doMainWork(new Runnable() {
                @Override
                public void run() {
                    updateVideoSurfaces();
                }
            });
        }
    }

    public synchronized void attachViews() {
        if (mMediaPlayer == null) {
            init();
        }
        {
            mMediaPlayer.setEventListener(mEventListener);
            final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
            if (!vlcVout.areViewsAttached()) {
                vlcVout.addCallback(mCallback);
                vlcVout.setVideoView(mSurfaceView);
                vlcVout.attachViews(mOnNewVideoLayoutListener);
            }
        }
        mMediaPlayer.play();
    }

    public void detachViews() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            {
                IVLCVout vlcVout = mMediaPlayer.getVLCVout();
                if (vlcVout.areViewsAttached()) {
                    vlcVout.detachViews();
                }
            }
        }
    }

    public void release() {
        if (mMediaPlayer != null) {
            {
                Media media = mMediaPlayer.getMedia();
                if (media != null) {
                    if (!media.isReleased()) {
                        media.release();
                    }
                }
            }
            if (!mMediaPlayer.isReleased()) {
                mMediaPlayer.release();
            }
            mMediaPlayer = null;
        }
        if (mLibVlc != null) {
            if (!mLibVlc.isReleased()) {
                mLibVlc.release();
            }
            mLibVlc = null;
        }
    }

    private IVLCVout.Callback mCallback = new IVLCVout.Callback() {
        @Override
        public void onSurfacesCreated(IVLCVout vlcVout) {
        }

        @Override
        public void onSurfacesDestroyed(IVLCVout vlcVout) {
        }
    };

    private IVLCVout.OnNewVideoLayoutListener mOnNewVideoLayoutListener = new IVLCVout.OnNewVideoLayoutListener() {
        @Override
        public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
//            updateVideoSurfaces();
        }
    };

    public void updateVideoSurfaces() {

        final boolean isPortrait = mSurfaceView.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        double displayWidth;
        double displayHeight;
        String aspectRatio;
        if (isPortrait) {
            displayWidth = mPortraitDisplayWidth;
            displayHeight = mPortraitDisplayHeight;
        } else {
            displayWidth = mLandscapeDisplayWidth;
            displayHeight = mLandscapeDisplayHeight;

        }

        if (mMediaPlayer != null) {
            aspectRatio = displayWidth + ":" + displayHeight;
            mMediaPlayer.getVLCVout().setWindowSize((int) displayWidth, (int) displayHeight);
            mMediaPlayer.setAspectRatio(aspectRatio);
            mMediaPlayer.setScale(0);
        }

        ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
        layoutParams.width = (int) displayWidth;
        layoutParams.height = (int) displayHeight;
        mSurfaceView.setLayoutParams(layoutParams);
    }

    public int getLandscapeDisplayHeight() {
        return mLandscapeDisplayHeight;
    }

    public void setLandscapeDisplayHeight(int landscapeDisplayHeight) {
        mLandscapeDisplayHeight = landscapeDisplayHeight;
    }

    public void setUriString(String uriString) {
        mUriString = uriString;
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public LibVLC getLibVlc() {
        return mLibVlc;
    }

    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }
}
