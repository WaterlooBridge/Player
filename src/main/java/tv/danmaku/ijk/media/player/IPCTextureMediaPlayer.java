package tv.danmaku.ijk.media.player;

import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Surface;

/**
 * Created by lin on 2020/7/18.
 */

public class IPCTextureMediaPlayer implements IIjkMediaPlayer, ISurfaceTextureHolder {

    private IIjkMediaPlayer mMediaPlayer;

    private SurfaceTexture mSurfaceTexture;
    private ISurfaceTextureHost mSurfaceTextureHost;

    public IPCTextureMediaPlayer(IIjkMediaPlayer player) {
        mMediaPlayer = player;
    }

    @Override
    public void setSurface(Surface surface) throws RemoteException {
//        mMediaPlayer.setSurface(surface);
    }

    @Override
    public void setOption(int category, String name, String value) throws RemoteException {
        mMediaPlayer.setOption(category, name, value);
    }

    @Override
    public void _setOption(int category, String name, long value) throws RemoteException {
        mMediaPlayer._setOption(category, name, value);
    }

    @Override
    public void openProxy() throws RemoteException {
        mMediaPlayer.openProxy();
    }

    @Override
    public void setDataSource(String path) throws RemoteException {
        mMediaPlayer.setDataSource(path);
    }

    @Override
    public void _setDataSource(Uri uri) throws RemoteException {
        mMediaPlayer._setDataSource(uri);
    }

    @Override
    public String getDataSource() throws RemoteException {
        return mMediaPlayer.getDataSource();
    }

    @Override
    public void prepareAsync() throws RemoteException {
        mMediaPlayer.prepareAsync();
    }

    @Override
    public void start() throws RemoteException {
        mMediaPlayer.start();
    }

    @Override
    public void stop() throws RemoteException {
        mMediaPlayer.stop();
    }

    @Override
    public void pause() throws RemoteException {
        mMediaPlayer.pause();
    }

    @Override
    public int getVideoWidth() throws RemoteException {
        return mMediaPlayer.getVideoWidth();
    }

    @Override
    public int getVideoHeight() throws RemoteException {
        return mMediaPlayer.getVideoHeight();
    }

    @Override
    public boolean isPlaying() throws RemoteException {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public void seekTo(long msec) throws RemoteException {
        mMediaPlayer.seekTo(msec);
    }

    @Override
    public long getCurrentPosition() throws RemoteException {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() throws RemoteException {
        return mMediaPlayer.getDuration();
    }

    @Override
    public void release() throws RemoteException {
        mMediaPlayer.release();
    }

    @Override
    public void reset() throws RemoteException {
        mMediaPlayer.reset();
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) throws RemoteException {
        mMediaPlayer.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void setAudioStreamType(int streamtype) throws RemoteException {
        mMediaPlayer.setAudioStreamType(streamtype);
    }

    @Override
    public int getVideoSarNum() throws RemoteException {
        return mMediaPlayer.getVideoSarNum();
    }

    @Override
    public int getVideoSarDen() throws RemoteException {
        return mMediaPlayer.getVideoSarDen();
    }

    @Override
    public void registerCallback(IPlayCallback callback) throws RemoteException {
        mMediaPlayer.registerCallback(callback);
    }

    @Override
    public IBinder asBinder() {
        return mMediaPlayer.asBinder();
    }

    @Override
    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        if (mSurfaceTexture == surfaceTexture)
            return;

        releaseSurfaceTexture();
        mSurfaceTexture = surfaceTexture;
        try {
            if (surfaceTexture == null) {
                mMediaPlayer.setSurface(null);
            } else {
                mMediaPlayer.setSurface(new Surface(surfaceTexture));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    public void setSurfaceTextureHost(ISurfaceTextureHost surfaceTextureHost) {
        mSurfaceTextureHost = surfaceTextureHost;
    }

    public void releaseSurfaceTexture() {
        if (mSurfaceTexture != null) {
            if (mSurfaceTextureHost != null) {
                mSurfaceTextureHost.releaseSurfaceTexture(mSurfaceTexture);
            } else {
                mSurfaceTexture.release();
            }
            mSurfaceTexture = null;
        }
    }
}
