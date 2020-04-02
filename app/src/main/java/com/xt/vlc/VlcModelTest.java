package com.xt.vlc;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;

/**
 * created by XuTi on 2019/5/20 14:00
 */
public class VlcModelTest {
    private static final String TAG = VlcModelTest.class.getSimpleName();

    private final Handler mHandler = new Handler();

    private LibVLC mLibVLC;
    private MediaPlayer mMediaPlayer;

    private FrameLayout mVideoSurfaceFrame = null;
    private SurfaceView mVideoSurface      = null;
    private SurfaceView mSubtitlesSurface  = null;
    private TextureView mVideoTexture      = null;
    private View        mVideoView         = null;

    private static final boolean USE_SURFACE_VIEW   = true;
    private static final boolean ENABLE_SUBTITLES   = true;
    private static final String  ASSET_FILENAME     = "bbb.m4v";
    private static final int     SURFACE_BEST_FIT   = 0;
    private static final int     SURFACE_FIT_SCREEN = 1;
    private static final int     SURFACE_FILL       = 2;
    private static final int     SURFACE_16_9       = 3;
    private static final int     SURFACE_4_3        = 4;
    private static final int     SURFACE_ORIGINAL   = 5;
    private static       int     sCurrentSize       = SURFACE_BEST_FIT;

    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private int mVideoVisibleWidth = 0;
    private int mVideoVisibleHeight = 0;
    private int mVideoSarNum = 0;
    private int mVideoSarDen = 0;

    public void onCreate(final Activity activity) {
        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        mLibVLC = new LibVLC(activity, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);

        mVideoSurfaceFrame = activity.findViewById(R.id.video_surface_frame);
        if (USE_SURFACE_VIEW) {
            ViewStub stub = activity.findViewById(R.id.surface_stub);
            mVideoSurface = (SurfaceView) stub.inflate();
            if (ENABLE_SUBTITLES) {
                stub = activity.findViewById(R.id.subtitles_surface_stub);
                mSubtitlesSurface = (SurfaceView) stub.inflate();
                mSubtitlesSurface.setZOrderMediaOverlay(true);
                mSubtitlesSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            }
            mVideoView = mVideoSurface;
        } else {
            ViewStub stub = activity.findViewById(R.id.texture_stub);
            mVideoTexture = (TextureView) stub.inflate();
            mVideoView = mVideoTexture;
        }
    }

    public void onStart() {
        final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        if (mVideoSurface != null) {
            vlcVout.setVideoView(mVideoSurface);
            if (mSubtitlesSurface != null) {
                vlcVout.setSubtitlesView(mSubtitlesSurface);
            }
        } else {
            vlcVout.setVideoView(mVideoTexture);
        }
        vlcVout.attachViews(mOnNewVideoLayoutListener);
        final Media media = new Media(mLibVLC, Uri.parse("rtsp://192.168.1.254/xxxx.mov"));
        media.setHWDecoderEnabled(true, true);
        media.addOption(":network-caching=250");
        mMediaPlayer.setMedia(media);
        media.release();
        mMediaPlayer.play();

        mVideoSurfaceFrame.addOnLayoutChangeListener(mOnLayoutChangeListener);
    }

    public void onStop() {
        if (mOnLayoutChangeListener != null) {
            mVideoSurfaceFrame.removeOnLayoutChangeListener(mOnLayoutChangeListener);
        }

        mMediaPlayer.stop();

        mMediaPlayer.getVLCVout().detachViews();
    }

    public void onDestroy() {
        mMediaPlayer.release();
        mLibVLC.release();
    }

    private View.OnLayoutChangeListener mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
        private final Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                updateVideoSurfaces();
            }
        };

        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                                   int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                mHandler.removeCallbacks(mRunnable);
                mHandler.post(mRunnable);
            }
        }
    };

    private IVLCVout.OnNewVideoLayoutListener mOnNewVideoLayoutListener = new IVLCVout.OnNewVideoLayoutListener() {
        @Override
        public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
            mVideoWidth = width;
            mVideoHeight = height;

            mVideoVisibleWidth = visibleWidth;
            mVideoVisibleHeight = visibleHeight;

            mVideoSarNum = sarNum;
            mVideoSarDen = sarDen;

            updateVideoSurfaces();
        }
    };

    private void updateVideoSurfaces() {
        Activity activity;
        if (mVideoView.getContext() instanceof Activity) {
            activity = (Activity) mVideoView.getContext();
        } else {
            return;
        }
        int screenWidth = activity.getWindow().getDecorView().getWidth();
        int screenHeight = activity.getWindow().getDecorView().getHeight();

        // sanity check
        if (screenWidth * screenHeight == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }

        mMediaPlayer.getVLCVout().setWindowSize(screenWidth, screenHeight);

        ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();
        if (mVideoWidth * mVideoHeight == 0) {
            /* Case of OpenGL vouts: handles the placement of the video using MediaPlayer API */
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mVideoView.setLayoutParams(lp);

            lp = mVideoSurfaceFrame.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mVideoSurfaceFrame.setLayoutParams(lp);

            changeMediaPlayerLayout(screenWidth, screenHeight);
            return;
        }

        if (lp.width == lp.height && lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            /* We handle the placement of the video using Android View LayoutParams */
            mMediaPlayer.setAspectRatio(null);
            mMediaPlayer.setScale(0);
        }

        double displayW = screenWidth;
        double displayH = screenHeight;
        final boolean isPortrait = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        boolean b = (screenWidth > screenHeight) && isPortrait;
        boolean b1 = (screenWidth < screenHeight) && !isPortrait;
        if (b || b1) {
            displayW = screenHeight;
            displayH = screenWidth;
        }

        // compute the aspect ratio
        double videoAspectRatio;
        double videoVisibleW;
        if (mVideoSarDen == mVideoSarNum) {
            /* No indication about the density, assuming 1:1 */
            videoVisibleW = mVideoVisibleWidth;
            videoAspectRatio = (double) mVideoVisibleWidth / (double) mVideoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            videoVisibleW = mVideoVisibleWidth * (double) mVideoSarNum / mVideoSarDen;
            videoAspectRatio = videoVisibleW / mVideoVisibleHeight;
        }

        // compute the display aspect ratio
        double displayAspectRatio = displayW / displayH;

        switch (sCurrentSize) {
            case SURFACE_BEST_FIT:
                if (displayAspectRatio < videoAspectRatio) {
                    displayH = displayW / videoAspectRatio;
                } else {
                    displayW = displayH * videoAspectRatio;
                }
                break;
            case SURFACE_FIT_SCREEN:
                if (displayAspectRatio >= videoAspectRatio) {
                    //horizontal
                    displayH = displayW / videoAspectRatio;
                } else {
                    //vertical
                    displayW = displayH * videoAspectRatio;
                }
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                videoAspectRatio = 16.0 / 9.0;
                if (displayAspectRatio < videoAspectRatio) {
                    displayH = displayW / videoAspectRatio;
                } else {
                    displayW = displayH * videoAspectRatio;
                }
                break;
            case SURFACE_4_3:
                videoAspectRatio = 4.0 / 3.0;
                if (displayAspectRatio < videoAspectRatio) {
                    displayH = displayW / videoAspectRatio;
                } else {
                    displayW = displayH * videoAspectRatio;
                }
                break;
            case SURFACE_ORIGINAL:
                displayH = mVideoVisibleHeight;
                displayW = videoVisibleW;
                break;
            default:
                break;
        }

        // set display size
        lp.width = (int) Math.ceil(displayW * mVideoWidth / mVideoVisibleWidth);
        lp.height = (int) Math.ceil(displayH * mVideoHeight / mVideoVisibleHeight);
        mVideoView.setLayoutParams(lp);
        if (mSubtitlesSurface != null) {
            mSubtitlesSurface.setLayoutParams(lp);
        }

        // set frame size (crop if necessary)
        lp = mVideoSurfaceFrame.getLayoutParams();
        lp.width = (int) Math.floor(displayW);
        lp.height = (int) Math.floor(displayH);
        mVideoSurfaceFrame.setLayoutParams(lp);

        mVideoView.invalidate();
        if (mSubtitlesSurface != null) {
            mSubtitlesSurface.invalidate();
        }

        changeMediaPlayerLayout((int) displayW, (int) displayH);
    }

    private void changeMediaPlayerLayout(int displayW, int displayH) {
        //Change the video placement using the MediaPlayer API
        switch (sCurrentSize) {
            case SURFACE_BEST_FIT:
                mMediaPlayer.setAspectRatio(null);
                mMediaPlayer.setScale(0);
                break;
            case SURFACE_FIT_SCREEN:
            case SURFACE_FILL: {
                Media.VideoTrack videoTrack = mMediaPlayer.getCurrentVideoTrack();
                if (videoTrack == null) {
                    return;
                }
                final boolean videoSwapped = videoTrack.orientation == Media.VideoTrack.Orientation.LeftBottom
                        || videoTrack.orientation == Media.VideoTrack.Orientation.RightTop;
                int videoW = videoTrack.width;
                int videoH = videoTrack.height;
                if (sCurrentSize == SURFACE_FIT_SCREEN) {

                    if (videoSwapped) {
                        int swap = videoW;
                        videoW = videoH;
                        videoH = swap;
                    }
                    if (videoTrack.sarNum != videoTrack.sarDen) {
                        videoW = videoW * videoTrack.sarNum / videoTrack.sarDen;
                    }

                    float ar = videoW / (float) videoH;
                    float dar = displayW / (float) displayH;

                    float scale;
                    if (dar >= ar) {
                        //horizontal
                        scale = displayW / (float) videoW;
                    } else {
                        //vertical
                        scale = displayH / (float) videoH;
                    }
                    mMediaPlayer.setScale(scale);
                    mMediaPlayer.setAspectRatio(null);
                } else {
                    mMediaPlayer.setScale(0);
                    mMediaPlayer.setAspectRatio(!videoSwapped ? "" + displayW + ":" + displayH
                            : "" + displayH + ":" + displayW);
                }
                break;
            }
            case SURFACE_16_9:
                mMediaPlayer.setAspectRatio("16:9");
                mMediaPlayer.setScale(0);
                break;
            case SURFACE_4_3:
                mMediaPlayer.setAspectRatio("4:3");
                mMediaPlayer.setScale(0);
                break;
            case SURFACE_ORIGINAL:
                mMediaPlayer.setAspectRatio(null);
                mMediaPlayer.setScale(1);
                break;
            default:
                break;
        }
    }
}
