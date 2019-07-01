package tv.danmaku.ijk.media.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import tv.danmaku.ijk.media.player.IIjkMediaPlayer;
import tv.danmaku.ijk.media.player.IPlayerFactory;

public class PlayerServiceManager {

    private volatile static PlayerServiceManager instance;

    public static PlayerServiceManager getInstance(Context context) {
        if (instance == null)
            synchronized (PlayerServiceManager.class) {
                if (instance == null)
                    instance = new PlayerServiceManager(context.getApplicationContext());
            }
        return instance;
    }

    private Context context;
    private ServiceConnection conn;
    private IPlayerFactory factory;

    private PlayerServiceManager(Context context) {
        this.context = context;
    }

    public void createPlayer(PlayerCreateCallback callback) {
        if (conn == null) {
            conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    factory = IPlayerFactory.Stub.asInterface(service);
                    createPlayerInternal(callback);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    factory = null;
                    conn = null;
                }
            };
            Intent intent = new Intent(context, IjkMediaPlayerService.class);
            context.getApplicationContext().bindService(intent, conn, Context.BIND_AUTO_CREATE);
        } else {
            createPlayerInternal(callback);
        }
    }

    private void createPlayerInternal(PlayerCreateCallback callback) {
        if (factory == null)
            return;
        try {
            callback.onPlayerCreate(factory.createPlayer());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public interface PlayerCreateCallback {
        void onPlayerCreate(IIjkMediaPlayer player);
    }
}
