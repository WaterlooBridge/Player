package tv.danmaku.ijk.media.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.view.Surface;

import tv.danmaku.ijk.media.player.IIjkMediaPlayer;
import tv.danmaku.ijk.media.player.IPlayCallback;
import tv.danmaku.ijk.media.player.IPlayerFactory;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IjkMediaPlayerService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new IPlayerFactory();
    }

    private static class IPlayerFactory extends tv.danmaku.ijk.media.player.IPlayerFactory.Stub {
        @Override
        public IIjkMediaPlayer createPlayer() throws RemoteException {
            return new IjkMediaPlayerStub();
        }
    }

    private static class IjkMediaPlayerStub extends IIjkMediaPlayer.Stub {

        private IjkMediaPlayer mMediaPlayer;
        private Handler handler;

        public IjkMediaPlayerStub() {
            mMediaPlayer = createPlayer();
            handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void setSurface(Surface surface) throws RemoteException {
            handler.post(() -> mMediaPlayer.setSurface(surface));
        }

        @Override
        public void setOption(int category, String name, String value) throws RemoteException {
            handler.post(() -> mMediaPlayer.setOption(category, name, value));
        }

        @Override
        public void _setOption(int category, String name, long value) throws RemoteException {
            handler.post(() -> mMediaPlayer.setOption(category, name, value));
        }

        @Override
        public void setDataSource(String path) throws RemoteException {
            handler.post(() -> {
                try {
                    mMediaPlayer.setDataSource(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public String getDataSource() throws RemoteException {
            return mMediaPlayer.getDataSource();
        }

        @Override
        public void prepareAsync() throws RemoteException {
            handler.post(() -> mMediaPlayer.prepareAsync());
        }

        @Override
        public void start() throws RemoteException {
            handler.post(() -> mMediaPlayer.start());
        }

        @Override
        public void stop() throws RemoteException {
            handler.post(() -> mMediaPlayer.stop());
        }

        @Override
        public void pause() throws RemoteException {
            handler.post(() -> mMediaPlayer.pause());
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
            handler.post(() -> mMediaPlayer.seekTo(msec));
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
            handler.post(() -> mMediaPlayer.release());
        }

        @Override
        public void reset() throws RemoteException {
            handler.post(() -> mMediaPlayer.reset());
        }

        @Override
        public void setVolume(float leftVolume, float rightVolume) throws RemoteException {
            handler.post(() -> mMediaPlayer.setVolume(leftVolume, rightVolume));
        }

        @Override
        public void setAudioStreamType(int streamtype) throws RemoteException {
            handler.post(() -> mMediaPlayer.setAudioStreamType(streamtype));
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
            handler.post(() -> {
                mMediaPlayer.setOnPreparedListener(mp -> {
                    try {
                        callback.onPrepared();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
                mMediaPlayer.setOnCompletionListener(mp -> {
                    try {
                        callback.onCompletion();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
                mMediaPlayer.setOnBufferingUpdateListener((mp, percent) -> {
                    try {
                        callback.onBufferingUpdate(percent);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
                mMediaPlayer.setOnSeekCompleteListener(mp -> {
                    try {
                        callback.onSeekComplete();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
                mMediaPlayer.setOnVideoSizeChangedListener((mp, width, height, sar_num, sar_den) -> {
                    try {
                        callback.onVideoSizeChanged(width, height, sar_num, sar_den);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
                mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    try {
                        return callback.onError(what, extra);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return false;
                });
                mMediaPlayer.setOnInfoListener((mp, what, extra) -> {
                    try {
                        return callback.onInfo(what, extra);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return false;
                });
            });
        }

        private IjkMediaPlayer createPlayer() {
            IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer();
            ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
            return ijkMediaPlayer;
        }
    }
}
