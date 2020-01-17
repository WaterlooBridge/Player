package tv.danmaku.ijk.media.cache;

import okhttp3.OkHttpClient;

/**
 * Created by lin on 19-11-26.
 */
class HttpUtil {

    static OkHttpClient getClient() {
        return Holder.holder;
    }

    static void evictAll() {
        Holder.holder.connectionPool().evictAll();
    }

    private static class Holder {
        private static OkHttpClient holder = new OkHttpClient.Builder().build();
    }
}
