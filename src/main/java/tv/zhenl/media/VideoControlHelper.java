package tv.zhenl.media;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import tv.danmaku.ijk.media.player.R;

public class VideoControlHelper {

    //手指放下的位置
    protected int mDownPosition;

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

    private VideoPlayerView videoView;
    private Window window;
    private boolean isTouching = false;

    public VideoControlHelper(VideoPlayerView videoView, Window window) {
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
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = x - mDownX;
                float deltaY = y - mDownY;
                float absDeltaX = Math.abs(deltaX);
                float absDeltaY = Math.abs(deltaY);
                if (!mChangePosition && !mChangeVolume && !mBrightness) {
                    touchSurfaceMoveFullLogic(absDeltaX, absDeltaY);
                }
                touchSurfaceMove(deltaX, deltaY, y);

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouching = false;
                touchSurfaceUp();
                break;
        }
        return true;
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
                }
            }
        }
    }


    protected void touchSurfaceUp() {
        dismissProgressDialog();
        dismissBrightnessDialog();
        if (mChangePosition && videoView.isPlaying()) {
            videoView.seekTo(mSeekTimePosition);
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
}
