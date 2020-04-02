package com.xt.vlc;

import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

/**
 * Created by Administrator on 2017/4/25.
 */

public class VlcManager {
    private static VlcManager sVlcManager = null;

    private VlcManager(Context context, String mrl) {
        initVlc(context, mrl);
    }

    /**
     * @param mrl 例如"rtsp://192.168.1.1/live1.sdp"
     * @return
     */
    public static VlcManager getInstance(Context context, String mrl) {
        if (sVlcManager == null) {
            sVlcManager = new VlcManager(context, mrl);
        }
        return sVlcManager;
    }

    private int                       displayWidth;
    private int                       displayHeight;
    private int                       mVideoWidth;
    private int                       mVideoHight;
    private SurfaceView               mSurfaceView;
    private SurfaceHolder             mSurfaceHolder;
    private IVLCVout                  mVlcVout;
    private IVLCVout.Callback         mCallback;
    private MediaPlayer               mMediaPlayer;
    private MediaPlayer.EventListener mEventListener;
    private LibVLC                    mLibVLC;

    private void initVlc(Context context, String mrl) {
        mLibVLC = LibVLCUtil.getLibVLC(context, null);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setMedia(getMedia(mrl));
        mVlcVout = mMediaPlayer.getVLCVout();
        mCallback = new VlcCallback();
        mVlcVout.addCallback(mCallback);
    }

    public void attachView(SurfaceView surfaceView) {
        Point point = getPoint(surfaceView.getContext());
        this.displayWidth = point.x;
        this.displayHeight = point.y;

        this.mSurfaceView = surfaceView;
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        mVlcVout.setVideoView(surfaceView);
        mVlcVout.attachViews(mOnNewVideoLayoutListener);

        changeSurfaceViewSize(mVideoWidth, mVideoHight);
    }

    public void detachView() {
        stopPlay();
        mVlcVout.detachViews();
    }

    public void onDestroy() {
        mMediaPlayer.stop();
        mMediaPlayer.setEventListener(null);
        mVlcVout.removeCallback(mCallback);
        mVlcVout.detachViews();
        mMediaPlayer.release();
        mLibVLC.release();
        mLibVLC = null;
        mSurfaceHolder = null;
        mSurfaceView = null;
        sVlcManager = null;
    }

    public void play() {
        mMediaPlayer.getMedia().release();
        mMediaPlayer.play();
    }

    public void stopPlay() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
    }

    @NonNull
    private Media getMedia(String mrl) {
        Media media = new Media(mLibVLC, Uri.parse(mrl));
        media.setHWDecoderEnabled(true, true);
        media.addOption(":network-caching=250");
        return media;
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    private void addOption(String mrl) {
        Media media = new Media(mLibVLC, Uri.parse(mrl));
        media.setHWDecoderEnabled(true, true);
        media.addOption(":network-caching=250");
        media.addOption(":live-caching=0");
        media.addOption(":file-caching=0");
        media.addOption(":codec=mediacodec,iomx,all");
    }

    public void setEventListener(final OnEventListener onEventListener) {
        mMediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                //播放结束
                if (getMediaPlayer().getPlayerState() == Media.State.Ended) {
//                    mMediaPlayer.stop();
                    onEventListener.onEnded();
                } else if (getMediaPlayer().getPlayerState() == Media.State.Playing) {
                    onEventListener.onPlaying();
                }
            }
        });
        this.mEventListener = mEventListener;
    }

    /*****************SurfaceView宽高相关**********************/

    private IVLCVout.OnNewVideoLayoutListener mOnNewVideoLayoutListener = new IVLCVout.OnNewVideoLayoutListener() {
        @Override
        public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
            if (mVideoWidth == width && mVideoHight == height) {
                return;
            } else {
                changeSurfaceViewSize(width, height);
            }
        }
    };

    class VlcCallback implements IVLCVout.Callback {

        @Override
        public void onSurfacesCreated(IVLCVout ivlcVout) {
            play();
        }

        @Override
        public void onSurfacesDestroyed(IVLCVout ivlcVout) {
        }
    }

    /**
     * 总结：surfaceview的宽高比要等于视频的宽高比
     *
     * @param width
     * @param height
     */
    private void changeSurfaceViewSize(int width, int height) {
        try {
            mVideoWidth = width;
            mVideoHight = height;
            // calculate aspect ratio，视频的宽高比
            double ar = (double) mVideoWidth / (double) mVideoHight;
            // calculate display aspect ratio，视频显示区域的宽高比
            double dar = (double) displayWidth / displayHeight;
            mSurfaceHolder.setFixedSize(mVideoWidth, mVideoWidth);
            ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
            if (dar > ar) {
                layoutParams.height = displayHeight;
                layoutParams.width = (int) (layoutParams.height * ar);
            } else {
                layoutParams.width = displayWidth;
                layoutParams.height = (int) (layoutParams.width / ar);
            }
            mSurfaceView.setLayoutParams(layoutParams);
            mSurfaceView.invalidate();
        } catch (Exception e) {
            Log.d("vlc-newlayout", e.toString());
        }
    }

    public void changeSurfaceViewSize() {
        changeSurfaceViewSize(mVideoWidth, mVideoHight);
    }

    private Point getPoint(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display       display       = windowManager.getDefaultDisplay();
        Point         point         = new Point();
        display.getSize(point);
        return point;
    }
}
