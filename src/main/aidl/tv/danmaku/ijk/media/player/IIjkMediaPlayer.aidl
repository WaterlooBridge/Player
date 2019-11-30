// IIjkMediaPlayer.aidl
package tv.danmaku.ijk.media.player;

import android.view.Surface;
import tv.danmaku.ijk.media.player.IPlayCallback;

// Declare any non-default types here with import statements

interface IIjkMediaPlayer {

    void setSurface(in Surface surface);

    void setOption(int category, String name, String value);

    void _setOption(int category, String name, long value);

    void openProxy();

    void setDataSource(String path);

    void _setDataSource(in Uri uri);

    String getDataSource();

    void prepareAsync();

    void start();

    void stop();

    void pause();

    int getVideoWidth();

    int getVideoHeight();

    boolean isPlaying();

    void seekTo(long msec);

    long getCurrentPosition();

    long getDuration();

    void release();

    void reset();

    void setVolume(float leftVolume, float rightVolume);

    void setAudioStreamType(int streamtype);

    int getVideoSarNum();

    int getVideoSarDen();

    void registerCallback(IPlayCallback callback);
}
