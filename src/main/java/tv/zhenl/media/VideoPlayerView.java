package tv.zhenl.media;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.database.ExoDatabaseProvider;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.PlayerView;

import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.internal.connection.RealCall;
import tv.danmaku.ijk.media.player.R;

/**
 * Created by lin on 2022/7/10.
 */
public class VideoPlayerView extends PlayerView {

    private ExoPlayer mMediaPlayer;

    private OkHttpClient mClient;
    private String mUserAgent;

    public VideoPlayerView(Context context) {
        this(context, null);
    }

    public VideoPlayerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializePlayer();
    }

    private void initializePlayer() {
        mClient = new OkHttpClient.Builder().build();
        mMediaPlayer = new ExoPlayer.Builder(getContext()).build();
        setPlayer(mMediaPlayer);
    }

    public void setUserAgent(String userAgent) {
        mUserAgent = userAgent;
    }

    public void setVideoUri(Uri uri, Map<String, String> headers, int startPositionMs) {
        MediaSource.Factory mediaSourceFactory;
        if (uri.toString().startsWith("http")) {
            OkHttpDataSource.Factory okhttpDataSourceFactory = new OkHttpDataSource.Factory(request -> new RealCall(mClient, request, false));
            if (!TextUtils.isEmpty(mUserAgent)) okhttpDataSourceFactory.setUserAgent(mUserAgent);
            if (headers != null) okhttpDataSourceFactory.setDefaultRequestProperties(headers);

            SimpleCache cache = IPCPlayerControl.getCache(getContext());
            CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(okhttpDataSourceFactory);

            mediaSourceFactory = new DefaultMediaSourceFactory(cacheDataSourceFactory);
        } else {
            mediaSourceFactory = new DefaultMediaSourceFactory(getContext());
        }

        MediaItem item = MediaItem.fromUri(uri).buildUpon().build();
        MediaSource mediaSource = mediaSourceFactory.createMediaSource(item);
        if (startPositionMs == 0)
            mMediaPlayer.setMediaSource(mediaSource);
        else
            mMediaPlayer.setMediaSource(mediaSource, startPositionMs);
        mMediaPlayer.prepare();
    }

    public void setNextClickListener(OnClickListener listener) {
        View btn = findViewById(R.id.btn_next);
        if (btn != null) btn.setOnClickListener(listener);
    }

    public void addPlayerListener(Player.Listener listener) {
        mMediaPlayer.addListener(listener);
    }

    public void removePlayerListener(Player.Listener listener) {
        mMediaPlayer.removeListener(listener);
    }

    public int getDuration() {
        return (int) mMediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return (int) mMediaPlayer.getCurrentPosition();
    }

    public void seekTo(int positionMs) {
        mMediaPlayer.seekTo(positionMs);
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public void start() {
        mMediaPlayer.play();
        onResume();
        setKeepScreenOn(true);
    }

    public void pause() {
        mMediaPlayer.pause();
        onPause();
        setKeepScreenOn(false);
    }

    public void release(boolean clearTargetState) {
        mMediaPlayer.release();
        setPlayer(null);
    }
}
