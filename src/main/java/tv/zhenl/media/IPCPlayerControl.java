package tv.zhenl.media;

import android.content.Context;

import androidx.media3.database.ExoDatabaseProvider;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import java.io.File;

/**
 * Created by lin on 2020/7/28.
 */

public class IPCPlayerControl {

    private static final String IJK_CACHE_DIR = "/ijkiocache/";

    private static SimpleCache cache;

    public static SimpleCache getCache(Context context) {
        if (cache != null)
            return cache;
        cache = new SimpleCache(getCacheDir(context), new NoOpCacheEvictor(), new ExoDatabaseProvider(context));
        return cache;
    }

    public static File getCacheDir(Context context) {
        String basicPath = context.getExternalCacheDir().getPath() + IPCPlayerControl.IJK_CACHE_DIR;
        return new File(basicPath);
    }

    public static void clearCache(Context context) {
        if (cache != null) cache.release();
        SimpleCache.delete(getCacheDir(context), new ExoDatabaseProvider(context));
        cache = null;
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
