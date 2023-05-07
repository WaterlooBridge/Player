package tv.zhenl.media;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import java.io.IOException;

/**
 * Created by lin on 2022/7/23.
 */
public class DataSourceWrapper implements DataSource {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int MAX_SOURCE_ERROR_COUNT = 5;

    public static class Factory implements DataSource.Factory {

        private final DataSource.Factory delegate;

        public Factory(DataSource.Factory factory) {
            this.delegate = factory;
        }

        @NonNull
        @Override
        public DataSource createDataSource() {
            return new DataSourceWrapper(delegate.createDataSource());
        }
    }

    private final DataSource delegate;
    private int sourceErrorCount;
    private Uri uri;

    public DataSourceWrapper(DataSource source) {
        this.delegate = source;
    }

    @Override
    public void addTransferListener(@NonNull TransferListener transferListener) {
        delegate.addTransferListener(transferListener);
    }

    @Override
    public long open(@NonNull DataSpec dataSpec) throws IOException {
        this.uri = null;
        long bytesToRead = 0;
        try {
            bytesToRead = delegate.open(dataSpec);
            sourceErrorCount = 0;
        } catch (IOException e) {
            handleThrowable(dataSpec, e);
        }
        return bytesToRead;
    }

    private String previousErrorUrl;
    private int errorCount;

    private void handleThrowable(DataSpec dataSpec, IOException e) throws IOException {
        if (sourceErrorCount > MAX_SOURCE_ERROR_COUNT) throw e;

        boolean isHls = false;
        for (StackTraceElement element : e.getStackTrace()) {
            if (element.getClassName().contains("HlsMediaChunk")) {
                isHls = true;
                break;
            }
        }
        if (!isHls) throw e;

        String url = dataSpec.uri.toString();
        if (url.equals(previousErrorUrl)) {
            errorCount++;
        } else {
            previousErrorUrl = url;
            sourceErrorCount++;
            errorCount = 1;
        }
        if (errorCount <= MAX_RETRY_COUNT) throw e;

        this.uri = dataSpec.uri;
    }

    @Nullable
    @Override
    public Uri getUri() {
        Uri uri = delegate.getUri();
        if (uri == null && this.uri != null)
            return this.uri;
        return uri;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public int read(@NonNull byte[] buffer, int offset, int length) throws IOException {
        return delegate.read(buffer, offset, length);
    }
}
