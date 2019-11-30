package tv.danmaku.ijk.media.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import tv.danmaku.ijk.media.cache.HttpProxyCacheServer;
import tv.danmaku.ijk.media.player.IIjkMediaPlayer;
import tv.danmaku.ijk.media.player.IPlayCallback;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IjkMediaPlayerService extends Service {

    private static Context context;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        context = getApplicationContext();
        return new IPlayerFactory();
    }

    private static class IPlayerFactory extends tv.danmaku.ijk.media.player.IPlayerFactory.Stub {
        @Override
        public IIjkMediaPlayer createPlayer() throws RemoteException {
            return new IjkMediaPlayerStub();
        }
    }

    private static class IjkMediaPlayerStub extends IIjkMediaPlayer.Stub {

        private static final String IJKHTTPHOOK = "ijkhttphook:";

        private IjkMediaPlayer mMediaPlayer;
        private Handler handler;
        private Surface surface;

        public IjkMediaPlayerStub() {
            mMediaPlayer = createPlayer();
            handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void setSurface(Surface surface) throws RemoteException {
            this.surface = surface;
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
        public void openProxy() throws RemoteException {
            handler.post(() -> mMediaPlayer.setOnNativeInvokeListener((what, args) -> {
                if (what == IjkMediaPlayer.OnNativeInvokeListener.CTRL_WILL_HTTP_OPEN) {
                    Log.e("onNativeInvoke", args.toString());
                    String url = args.getString(IjkMediaPlayer.OnNativeInvokeListener.ARG_URL);
                    if (!TextUtils.isEmpty(url)) {
                        int index = url.indexOf(IJKHTTPHOOK);
                        if (index != -1)
                            url = url.substring(index + IJKHTTPHOOK.length());
                        args.putString(IjkMediaPlayer.OnNativeInvokeListener.ARG_URL, Holder.holder.getProxyUrl(url));
                    }
                }
                return false;
            }));
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
        public void _setDataSource(Uri uri) throws RemoteException {
            handler.post(() -> {
                try {
                    mMediaPlayer.setDataSource(context, uri);
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
            handler.post(() -> {
                mMediaPlayer.release();
                mMediaPlayer = null;
                if (surface != null) {
                    surface.release();
                    surface = null;
                }
            });
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

    private static class Holder {
        private static HttpProxyCacheServer holder = new HttpProxyCacheServer.Builder(context)
                .maxCacheSize(Long.MAX_VALUE)
                .build();
    }
}
