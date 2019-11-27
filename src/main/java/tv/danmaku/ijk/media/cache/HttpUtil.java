package tv.danmaku.ijk.media.cache;

import okhttp3.OkHttpClient;

/**
 * Created by lin on 19-11-26.
 */
class HttpUtil {

    public static OkHttpClient getClient() {
        return Holder.holder;
    }

    private static class Holder {
        private static OkHttpClient holder = new OkHttpClient.Builder().build();
    }
}
