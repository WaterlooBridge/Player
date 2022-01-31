package tv.danmaku.ijk.media.widget;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;
import java.util.Locale;

import tv.danmaku.ijk.media.player.R;

/**
 * Created by lin on 2018/3/2.
 */

public class AndroidMediaController extends FrameLayout implements IMediaController {
    private ActionBar mActionBar;

    private MediaController.MediaPlayerControl mPlayer;
    private Context mContext;
    private PopupWindow mWindow;
    private int mAnimStyle;
    private View mAnchor;
    private View mRoot;
    private SeekBar mProgress;
    private TextView mEndTime, mCurrentTime;
    private long mDuration;
    private boolean mShowing;
    private boolean mDragging;
    private boolean mInstantSeeking = true;
    private static int sDefaultTimeout = 3000;
    private static final int SEEK_TO_POST_DELAY_MILLIS = 200;

    private boolean mFromXml = false;
    private ImageButton mPauseButton;

    protected int mMediaPlayRes = Resources.getSystem().getIdentifier("ic_media_play", "drawable", "android");
    protected int mMediaPauseRes = Resources.getSystem().getIdentifier("ic_media_pause", "drawable", "android");

    private AudioManager mAM;
    private Runnable mLastSeekBarRunnable;
    private boolean mDisableProgress = false;
    private boolean isLock;

    public AndroidMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoot = this;
        mFromXml = true;
        initController(context);
    }

    public AndroidMediaController(Context context, boolean disableProgressBar) {
        this(context);
        mDisableProgress = disableProgressBar;
    }

    public AndroidMediaController(Context context) {
        super(context);
        if (!mFromXml && initController(context))
            initFloatingWindow();
    }

    public void setSupportActionBar(@Nullable ActionBar actionBar) {
        mActionBar = actionBar;
        if (actionBar == null)
            return;
        if (isShowing()) {
            actionBar.show();
        } else {
            actionBar.hide();
        }
    }

    public void setLock(boolean lock) {
        isLock = lock;
    }

    public boolean isLock() {
        return isLock;
    }

    @Override
    public void show() {
        if (isLock)
            return;
        show(sDefaultTimeout);
        if (mActionBar != null)
            mActionBar.show();
    }

    @Override
    public void hide() {
        hideInternal();
        if (mActionBar != null)
            mActionBar.hide();
        for (View view : mShowOnceArray)
            view.setVisibility(View.GONE);
        mShowOnceArray.clear();
    }

    private final ArrayList<View> mShowOnceArray = new ArrayList<>();

    public void showOnce(@NonNull View view) {
        mShowOnceArray.add(view);
        view.setVisibility(View.VISIBLE);
        show();
    }

    private boolean initController(Context context) {
        mContext = context.getApplicationContext();
        mAM = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return true;
    }

    @Override
    public void onFinishInflate() {
        if (mRoot != null)
            initControllerView(mRoot);
        super.onFinishInflate();
    }

    private void initFloatingWindow() {
        mWindow = new PopupWindow(mContext);
        mWindow.setFocusable(false);
        mWindow.setBackgroundDrawable(null);
        mWindow.setOutsideTouchable(true);
        mAnimStyle = android.R.style.Animation;
    }

    protected int getControllerLayoutId() {
        return R.layout.view_media_controller;
    }

    protected View makeControllerView() {
        return ((LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(getControllerLayoutId(), this);
    }

    private void initControllerView(View v) {
        mPauseButton = (ImageButton) v.findViewById(R.id.controller_stop_play);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }

        mProgress = (SeekBar) v.findViewById(R.id.controller_progress_bar);
        if (mProgress != null) {
            mProgress.setOnSeekBarChangeListener(mSeekListener);
            mProgress.setThumbOffset(1);
            mProgress.setMax(1000);
            mProgress.setEnabled(!mDisableProgress);
        }

        mEndTime = (TextView) v.findViewById(R.id.controller_end_time);
        mCurrentTime = (TextView) v.findViewById(R.id.controller_current_time);

        onViewCreated(v);
    }

    protected void onViewCreated(@NonNull View view) {
    }

    public void setInstantSeeking(boolean seekWhenDragging) {
        mInstantSeeking = seekWhenDragging;
    }

    private void disableUnsupportedButtons() {
        try {
            if (mPauseButton != null && !mPlayer.canPause())
                mPauseButton.setEnabled(false);
        } catch (IncompatibleClassChangeError ex) {
        }
    }

    public void setAnimationStyle(int animationStyle) {
        mAnimStyle = animationStyle;
    }


    public interface OnShownListener {
        void onShown();
    }

    private OnShownListener mShownListener;

    public void setOnShownListener(OnShownListener l) {
        mShownListener = l;
    }

    public interface OnHiddenListener {
        void onHidden();
    }

    private OnHiddenListener mHiddenListener;

    public void setOnHiddenListener(OnHiddenListener l) {
        mHiddenListener = l;
    }

    private final Handler mHandler = new Handler();

    private final Runnable mFadeOut = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private final Runnable mShowProgress = new Runnable() {
        @Override
        public void run() {
            long pos = setProgress();
            if (pos == -1)
                return;
            if (!mDragging && mShowing && mPlayer.isPlaying())
                mHandler.postDelayed(mShowProgress, 1000 - (pos % 1000));
            updatePausePlay();
        }
    };

    private long setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }

        long position = mPlayer.getCurrentPosition();
        long duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        mDuration = duration;

        if (mEndTime != null)
            mEndTime.setText(generateTime(mDuration));
        if (mCurrentTime != null)
            mCurrentTime.setText(generateTime(position));

        return position;
    }

    private static String generateTime(long position) {
        int totalSeconds = (int) (position / 1000);

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                show(0); // show until hide is called
                break;
            case MotionEvent.ACTION_UP:
                show(sDefaultTimeout); // start timeout
                break;
            case MotionEvent.ACTION_CANCEL:
                hide();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getRepeatCount() == 0
                && (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE)) {
            doPauseResume();
            show(sDefaultTimeout);
            if (mPauseButton != null)
                mPauseButton.requestFocus();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK
                || keyCode == KeyEvent.KEYCODE_MENU) {
            hide();
            return true;
        } else {
            show(sDefaultTimeout);
        }
        return super.dispatchKeyEvent(event);
    }

    private final OnClickListener mPauseListener = new OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            show(sDefaultTimeout);
        }
    };

    private void updatePausePlay() {
        if (mRoot == null || mPauseButton == null)
            return;

        if (mPlayer.isPlaying())
            mPauseButton.setImageResource(mMediaPauseRes);
        else
            mPauseButton.setImageResource(mMediaPlayRes);
    }

    private void doPauseResume() {
        if (mPlayer.isPlaying())
            mPlayer.pause();
        else
            mPlayer.start();
        updatePausePlay();
    }

    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

        public void onStartTrackingTouch(SeekBar bar) {
            mDragging = true;
            show(3600000);
            mHandler.removeCallbacks(mShowProgress);
            if (mInstantSeeking)
                mAM.setStreamMute(AudioManager.STREAM_MUSIC, true);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }

            final long newposition = (mDuration * progress) / 1000;
            String time = generateTime(newposition);
            if (mInstantSeeking) {
                mHandler.removeCallbacks(mLastSeekBarRunnable);
                mLastSeekBarRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mPlayer.seekTo((int) newposition);
                    }
                };
                mHandler.postDelayed(mLastSeekBarRunnable, SEEK_TO_POST_DELAY_MILLIS);
            }
            if (mCurrentTime != null)
                mCurrentTime.setText(time);
        }

        public void onStopTrackingTouch(SeekBar bar) {
            if (!mInstantSeeking)
                mPlayer.seekTo((int) (mDuration * bar.getProgress() / 1000));

            show(sDefaultTimeout);
            mHandler.removeCallbacks(mShowProgress);
            mAM.setStreamMute(AudioManager.STREAM_MUSIC, false);
            mDragging = false;
            mHandler.postDelayed(mShowProgress, 1000);
        }
    };

    @Override
    public void setAnchorView(View view) {
        mAnchor = view;
        if (mAnchor == null) {
            sDefaultTimeout = 0;
        }
        if (!mFromXml) {
            removeAllViews();
            mRoot = makeControllerView();
            mWindow.setContentView(mRoot);
            mWindow.setWidth(LayoutParams.MATCH_PARENT);
            mWindow.setHeight(LayoutParams.WRAP_CONTENT);
        }
        initControllerView(mRoot);
    }

    @Override
    public void setMediaPlayer(MediaController.MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    @Override
    public void show(int timeout) {
        if (!mShowing) {
            if (mPauseButton != null)
                mPauseButton.requestFocus();
            disableUnsupportedButtons();

            if (mFromXml) {
                setVisibility(View.VISIBLE);
            } else {
                if (mAnchor != null) {
                    mWindow.setAnimationStyle(mAnimStyle);
                    mRoot.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    mWindow.showAsDropDown(mAnchor, 0, -mRoot.getMeasuredHeight());
                } else {
                    mWindow.setAnimationStyle(mAnimStyle);
                    mWindow.showAtLocation(mRoot, Gravity.BOTTOM, 0, 0);
                }
            }
            mShowing = true;
            if (mShownListener != null)
                mShownListener.onShown();
        }
        updatePausePlay();
        mHandler.post(mShowProgress);

        if (timeout != 0) {
            mHandler.removeCallbacks(mFadeOut);
            mHandler.postDelayed(mFadeOut, timeout);
        }
    }

    @Override
    public boolean isShowing() {
        return mShowing;
    }

    public void hideInternal() {
        if (mShowing) {
            try {
                mHandler.removeCallbacks(mShowProgress);
                if (mFromXml)
                    setVisibility(View.GONE);
                else
                    mWindow.dismiss();
            } catch (IllegalArgumentException ex) {
            }
            mShowing = false;
            if (mHiddenListener != null)
                mHiddenListener.onHidden();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mProgress != null && !mDisableProgress)
            mProgress.setEnabled(enabled);
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    public void release() {
        hideInternal();
    }
}
