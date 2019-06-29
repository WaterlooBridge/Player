// IPlayCallback.aidl
package tv.danmaku.ijk.media.player;

// Declare any non-default types here with import statements

interface IPlayCallback {

    void onPrepared();

    void onCompletion();

    void onBufferingUpdate(int percent);

    void onSeekComplete();

    void onVideoSizeChanged(int width, int height, int sar_num, int sar_den);

    boolean onError(int what, int extra);

    boolean onInfo(int what, int extra);
}
