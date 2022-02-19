package tv.danmaku.ijk.media.video;

import static tv.danmaku.ijk.media.video.EGLSurfaceTexture.SECURE_MODE_NONE;
import static tv.danmaku.ijk.media.video.EGLSurfaceTexture.SECURE_MODE_PROTECTED_PBUFFER;
import static tv.danmaku.ijk.media.video.EGLSurfaceTexture.SECURE_MODE_SURFACELESS_CONTEXT;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import tv.danmaku.ijk.media.video.EGLSurfaceTexture.SecureMode;

/**
 * Created by lin on 2022/2/18.
 */
public final class DummySurface extends Surface {

    private static final String TAG = "DummySurface";

    /**
     * Whether the surface is secure.
     */
    public final boolean secure;

    private static @SecureMode
    int secureMode;
    private static boolean secureModeInitialized;

    private final DummySurfaceThread thread;
    private boolean threadReleased;

    /**
     * Returns whether the device supports secure dummy surfaces.
     *
     * @param context Any {@link Context}.
     * @return Whether the device supports secure dummy surfaces.
     */
    public static synchronized boolean isSecureSupported(Context context) {
        if (!secureModeInitialized) {
            secureMode = getSecureMode(context);
            secureModeInitialized = true;
        }
        return secureMode != SECURE_MODE_NONE;
    }

    /**
     * Returns a newly created dummy surface. The surface must be released by calling {@link #release}
     * when it's no longer required.
     *
     * @param context Any {@link Context}.
     * @param secure  Whether a secure surface is required. Must only be requested if {@link
     *                #isSecureSupported(Context)} returns {@code true}.
     * @throws IllegalStateException If a secure surface is requested on a device for which {@link
     *                               #isSecureSupported(Context)} returns {@code false}.
     */
    public static DummySurface newInstanceV17(Context context, boolean secure) {
        DummySurfaceThread thread = new DummySurfaceThread();
        return thread.init(secure ? secureMode : SECURE_MODE_NONE);
    }

    private DummySurface(DummySurfaceThread thread, SurfaceTexture surfaceTexture, boolean secure) {
        super(surfaceTexture);
        this.thread = thread;
        this.secure = secure;
    }

    @Override
    public void release() {
        super.release();
        // The Surface may be released multiple times (explicitly and by Surface.finalize()). The
        // implementation of super.release() has its own deduplication logic. Below we need to
        // deduplicate ourselves. Synchronization is required as we don't control the thread on which
        // Surface.finalize() is called.
        synchronized (thread) {
            if (!threadReleased) {
                thread.release();
                threadReleased = true;
            }
        }
    }

    @SecureMode
    private static int getSecureMode(Context context) {
        if (GlUtil.isProtectedContentExtensionSupported(context)) {
            if (GlUtil.isSurfacelessContextExtensionSupported()) {
                return SECURE_MODE_SURFACELESS_CONTEXT;
            } else {
                // If we can't use surfaceless contexts, we use a protected 1 * 1 pixel buffer surface.
                // This may require support for EXT_protected_surface, but in practice it works on some
                // devices that don't have that extension. See also
                // https://github.com/google/ExoPlayer/issues/3558.
                return SECURE_MODE_PROTECTED_PBUFFER;
            }
        } else {
            return SECURE_MODE_NONE;
        }
    }

    private static class DummySurfaceThread extends HandlerThread implements Handler.Callback {

        private static final int MSG_INIT = 1;
        private static final int MSG_RELEASE = 2;

        private EGLSurfaceTexture eglSurfaceTexture;
        private Handler handler;
        @Nullable
        private Error initError;
        @Nullable
        private RuntimeException initException;
        @Nullable
        private DummySurface surface;

        public DummySurfaceThread() {
            super("ExoPlayer:DummySurface");
        }

        public DummySurface init(@SecureMode int secureMode) {
            start();
            handler = new Handler(getLooper(), /* callback= */ this);
            eglSurfaceTexture = new EGLSurfaceTexture(handler);
            boolean wasInterrupted = false;
            synchronized (this) {
                handler.obtainMessage(MSG_INIT, secureMode, 0).sendToTarget();
                while (surface == null && initException == null && initError == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        wasInterrupted = true;
                    }
                }
            }
            if (wasInterrupted) {
                // Restore the interrupted status.
                Thread.currentThread().interrupt();
            }
            if (initException != null) {
                throw initException;
            } else if (initError != null) {
                throw initError;
            } else {
                return surface;
            }
        }

        public void release() {
            handler.sendEmptyMessage(MSG_RELEASE);
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT:
                    try {
                        initInternal(/* secureMode= */ msg.arg1);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Failed to initialize dummy surface", e);
                        initException = e;
                    } catch (Error e) {
                        Log.e(TAG, "Failed to initialize dummy surface", e);
                        initError = e;
                    } finally {
                        synchronized (this) {
                            notify();
                        }
                    }
                    return true;
                case MSG_RELEASE:
                    try {
                        releaseInternal();
                    } catch (Throwable e) {
                        Log.e(TAG, "Failed to release dummy surface", e);
                    } finally {
                        quit();
                    }
                    return true;
                default:
                    return true;
            }
        }

        private void initInternal(@SecureMode int secureMode) {
            eglSurfaceTexture.init(secureMode);
            this.surface =
                    new DummySurface(
                            this, eglSurfaceTexture.getSurfaceTexture(), secureMode != SECURE_MODE_NONE);
        }

        private void releaseInternal() {
            eglSurfaceTexture.release();
        }
    }
}
