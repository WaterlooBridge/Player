package tv.danmaku.ijk.media.services;

import android.content.Context;

import java.io.File;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by lin on 2020/7/28.
 */

public class IPCPlayerControl {

    public static final String IJK_CACHE_DIR = "/ijkiocache/";

    static int logLevel = IjkMediaPlayer.IJK_LOG_UNKNOWN;

    /**
     * IjkMediaPlayerService runs in another process, so this method should be called
     * when {@link android.app.Application#onCreate()}.
     *
     * @param level {@link IjkMediaPlayer#IJK_LOG_DEBUG}
     */
    public static void setLogLevel(int level) {
        logLevel = level;
    }

    public static boolean clearCache(Context context) {
        if (context.getExternalCacheDir() == null)
            return false;
        String basicPath = context.getExternalCacheDir().getPath() + IPCPlayerControl.IJK_CACHE_DIR;
        return deleteDir(new File(basicPath));
    }

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null)
                for (File child : children) {
                    boolean success = deleteDir(child);
                    if (!success)
                        return false;
                }
        }
        return dir.delete();
    }

    public static long getCacheSize(Context context) {
        if (context.getExternalCacheDir() == null)
            return 0;
        String basicPath = context.getExternalCacheDir().getPath() + IPCPlayerControl.IJK_CACHE_DIR;
        return getFolderSize(new File(basicPath));
    }

    private static long getFolderSize(File dir) {
        long size = 0;
        File[] children = dir.listFiles();
        if (children != null)
            for (File child : children) {
                if (child.isDirectory())
                    size += getFolderSize(child);
                else
                    size += child.length();
            }
        return size;
    }
}
