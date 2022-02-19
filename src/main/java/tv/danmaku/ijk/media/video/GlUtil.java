package tv.danmaku.ijk.media.video;

import android.content.Context;
import android.content.pm.PackageManager;
import android.opengl.EGL14;
import android.opengl.EGLDisplay;
import android.os.Build;

import androidx.annotation.Nullable;

import javax.microedition.khronos.egl.EGL10;

/**
 * Created by lin on 2022/2/18.
 */
class GlUtil {

    private static final String EXTENSION_PROTECTED_CONTENT = "EGL_EXT_protected_content";
    private static final String EXTENSION_SURFACELESS_CONTEXT = "EGL_KHR_surfaceless_context";

    /**
     * Returns whether creating a GL context with {@value #EXTENSION_PROTECTED_CONTENT} is possible.
     * If {@code true}, the device supports a protected output path for DRM content when using GL.
     */
    public static boolean isProtectedContentExtensionSupported(Context context) {
        if (Build.VERSION.SDK_INT < 24) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 26
                && !context
                .getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE)) {
            // Pre API level 26 devices were not well tested unless they supported VR mode.
            return false;
        }

        EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        @Nullable String eglExtensions = EGL14.eglQueryString(display, EGL10.EGL_EXTENSIONS);
        return eglExtensions != null && eglExtensions.contains(EXTENSION_PROTECTED_CONTENT);
    }

    /**
     * Returns whether creating a GL context with {@value #EXTENSION_SURFACELESS_CONTEXT} is possible.
     */
    public static boolean isSurfacelessContextExtensionSupported() {
        EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        @Nullable String eglExtensions = EGL14.eglQueryString(display, EGL10.EGL_EXTENSIONS);
        return eglExtensions != null && eglExtensions.contains(EXTENSION_SURFACELESS_CONTEXT);
    }
}
