package androidx.media3.exoplayer.hls;

import android.util.Log;

import java.lang.reflect.Field;

/**
 * Created by lin on 2022/7/16.
 */
public class HlsExtractorFactoryHelper {

    public static void replaceDefaultHlsExtractorFactory() {
        try {
            Field field = HlsExtractorFactory.class.getDeclaredField("DEFAULT");
            field.setAccessible(true);
            field.set(null, new ShadowHlsExtractorFactory());
        } catch (Throwable e) {
            Log.e("HlsExtractorFactory", "replace fail", e);
        }
    }
}
