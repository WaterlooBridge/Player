package tv.zhenl.media;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import tv.danmaku.ijk.media.player.R;

public class VideoControlHelper {

    private static final int LONG_PRESS = 1;

    //手指放下的位置
    protected int mDownPosition;

    //手势调节音量的大小
    protected int mGestureDownVolume;

    //手势偏差值
    protected int mThreshold = 80;

    //手动改变滑动的位置
    protected int mSeekTimePosition;

    //手动滑动的起始偏移位置
    protected int mSeekEndOffset;

    protected int mSeekMaxTime = 120 * 1000;

    //触摸的X
    protected float mDownX;

    //触摸的Y
    protected float mDownY;

    //移动的Y
    protected float mMoveY;

    //亮度
    protected float mBrightnessData = -1;

    //触摸滑动进度的比例系数
    protected float mSeekRatio = 1;

    //是否改变音量
    protected boolean mChangeVolume = false;

    //是否改变播放进度
    protected boolean mChangePosition = false;

    //是否改变亮度
    protected boolean mBrightness = false;

    //是否首次触摸
    protected boolean mFirstTouch = false;

    private final Handler mHandler = new GestureHandler();

    private boolean mInLongPress;

    protected AudioManager mAudioManager;

    private VideoPlayerView videoView;
    private Window window;
    private boolean isTouching = false;

    public VideoControlHelper(VideoPlayerView videoView, Window window) {
        mAudioManager = (AudioManager) videoView.getContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mSeekEndOffset = CommonUtil.dip2px(videoView.getContext(), 50);
        this.videoView = videoView;
        this.window = window;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isTouching)
                    return false;
                isTouching = true;
                videoView.onTouchEvent(ev);
                touchSurfaceDown(x, y);

                mHandler.removeMessages(LONG_PRESS);
                mHandler.sendMessageAtTime(mHandler.obtainMessage(LONG_PRESS),
                        ev.getDownTime() + ViewConfiguration.getLongPressTimeout());
                break;
            case MotionEvent.ACTION_MOVE:
                if (mInLongPress) break;

                float deltaX = x - mDownX;
                float deltaY = y - mDownY;
                float absDeltaX = Math.abs(deltaX);
                float absDeltaY = Math.abs(deltaY);
                if (!mChangePosition && !mChangeVolume && !mBrightness) {
                    touchSurfaceMoveFullLogic(absDeltaX, absDeltaY);
                }
                touchSurfaceMove(deltaX, deltaY, y);

                if (isConsumed()) {
                    mHandler.removeMessages(LONG_PRESS);
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouching = false;
                touchSurfaceUp();
                break;
        }
        return true;
    }

    public boolean isConsumed() {
        return mChangePosition || mBrightness || mChangeVolume || mInLongPress;
    }

    protected void touchSurfaceDown(float x, float y) {
        mDownX = x;
        mDownY = y;
        mMoveY = 0;
        mChangeVolume = false;
        mChangePosition = false;
        mBrightness = false;
        mFirstTouch = true;
    }

    protected void touchSurfaceMove(float deltaX, float deltaY, float y) {
        int curWidth = videoView.getWidth();
        int curHeight = videoView.getHeight();

        if (mChangePosition) {
            int totalTimeDuration = videoView.getDuration();
            mSeekTimePosition = (int) (mDownPosition + (deltaX * mSeekMaxTime / curWidth) / mSeekRatio);
            if (mSeekTimePosition > totalTimeDuration)
                mSeekTimePosition = totalTimeDuration;
            else if (mSeekTimePosition < 0)
                mSeekTimePosition = 0;
            String seekTime = CommonUtil.stringForTime(mSeekTimePosition);
            String totalTime = CommonUtil.stringForTime(totalTimeDuration);
            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
        } else if (mChangeVolume) {
            deltaY = -deltaY;
            int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int deltaV = (int) (max * deltaY * 3 / curHeight);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
            int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / curHeight);

            showVolumeDialog(-deltaY, volumePercent);
        } else if (mBrightness) {
            if (Math.abs(deltaY) > mThreshold) {
                float percent = (-deltaY / curHeight);
                onBrightnessSlide(percent);
                mDownY = y;
            }
        }
    }

    protected void touchSurfaceMoveFullLogic(float absDeltaX, float absDeltaY) {
        int curWidth = videoView.getWidth();

        if (absDeltaX > mThreshold || absDeltaY > mThreshold) {
            if (absDeltaX >= mThreshold) {
                //防止全屏虚拟按键
                int screenWidth = videoView.getWidth();
                if (Math.abs(screenWidth - mDownX) > mSeekEndOffset) {
                    mChangePosition = true;
                    mDownPosition = videoView.getCurrentPosition();
                }
            } else {
                int screenHeight = videoView.getHeight();
                boolean noEnd = Math.abs(screenHeight - mDownY) > mSeekEndOffset;
                if (mFirstTouch) {
                    mBrightness = (mDownX < curWidth * 0.5f) && noEnd;
                    mFirstTouch = false;
                }
                if (!mBrightness) {
                    mChangeVolume = noEnd;
                    mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
            }
        }
    }


    protected void touchSurfaceUp() {
        dismissProgressDialog();
        dismissBrightnessDialog();
        dismissVolumeDialog();
        if (mChangePosition && videoView.isPlaying()) {
            videoView.seekTo(mSeekTimePosition);
        }

        mHandler.removeMessages(LONG_PRESS);
        if (mInLongPress) {
            mInLongPress = false;
            videoView.setPlaybackSpeed(1);
        }
    }

    protected void onBrightnessSlide(float percent) {
        mBrightnessData = window.getAttributes().screenBrightness;
        if (mBrightnessData <= 0.00f) {
            mBrightnessData = 0.50f;
        } else if (mBrightnessData < 0.01f) {
            mBrightnessData = 0.01f;
        }
        WindowManager.LayoutParams lpa = window.getAttributes();
        lpa.screenBrightness = mBrightnessData + percent;
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f;
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f;
        }
        showBrightnessDialog(lpa.screenBrightness);
        window.setAttributes(lpa);
    }

    private View mProgressDialog;
    private ProgressBar mDialogProgressBar;
    private TextView mDialogSeekTime;
    private TextView mDialogTotalTime;

    protected void showProgressDialog(float deltaX, String seekTime, int seekTimePosition, String totalTime, int totalTimeDuration) {
        if (mProgressDialog == null) {
            View localView = LayoutInflater.from(videoView.getContext()).inflate(R.layout.video_progress_dialog, videoView, false);
            mDialogProgressBar = localView.findViewById(R.id.duration_progressbar);
            mDialogSeekTime = localView.findViewById(R.id.tv_current);
            mDialogTotalTime = localView.findViewById(R.id.tv_duration);
            videoView.addView(localView);
            mProgressDialog = localView;
        }
        if (mProgressDialog.getVisibility() == View.GONE)
            mProgressDialog.setVisibility(View.VISIBLE);
        if (mDialogSeekTime != null)
            mDialogSeekTime.setText(seekTime);
        if (mDialogTotalTime != null)
            mDialogTotalTime.setText(" / " + totalTime);
        if (mDialogProgressBar != null && totalTimeDuration > 0)
            mDialogProgressBar.setProgress(seekTimePosition * 100 / totalTimeDuration);
    }

    protected void dismissProgressDialog() {
        if (mProgressDialog != null)
            mProgressDialog.setVisibility(View.GONE);
    }

    private View mBrightnessDialog;
    private TextView mBrightnessDialogTv;

    protected void showBrightnessDialog(float percent) {
        if (mBrightnessDialog == null) {
            View localView = LayoutInflater.from(videoView.getContext()).inflate(R.layout.video_brightness, videoView, false);
            mBrightnessDialogTv = localView.findViewById(R.id.app_video_brightness);
            videoView.addView(localView);
            mBrightnessDialog = localView;
        }
        if (mBrightnessDialog.getVisibility() == View.GONE)
            mBrightnessDialog.setVisibility(View.VISIBLE);
        if (mBrightnessDialogTv != null)
            mBrightnessDialogTv.setText((int) (percent * 100) + "%");
    }

    protected void dismissBrightnessDialog() {
        if (mBrightnessDialog != null)
            mBrightnessDialog.setVisibility(View.GONE);
    }

    private View mVolumeDialog;
    private TextView mVolumeDialogTv;

    protected void showVolumeDialog(float deltaY, int volumePercent) {
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(videoView.getContext()).inflate(R.layout.video_volume_dialog, videoView, false);
            mVolumeDialogTv = localView.findViewById(R.id.app_video_volume);
            videoView.addView(localView);
            mVolumeDialog = localView;
        }
        if (mVolumeDialog.getVisibility() == View.GONE)
            mVolumeDialog.setVisibility(View.VISIBLE);
        if (mVolumeDialogTv != null)
            mVolumeDialogTv.setText(Math.max(Math.min(volumePercent, 100), 0) + "%");
    }

    protected void dismissVolumeDialog() {
        if (mVolumeDialog != null)
            mVolumeDialog.setVisibility(View.GONE);
    }

    protected void dispatchLongPress() {
        mInLongPress = true;
        videoView.setPlaybackSpeed(2);
    }

    private class GestureHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == LONG_PRESS) {
                dispatchLongPress();
            }
        }
    }
}
