package tv.danmaku.ijk.media.cache;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import tv.danmaku.ijk.media.cache.headers.EmptyHeadersInjector;
import tv.danmaku.ijk.media.cache.headers.HeaderInjector;
import tv.danmaku.ijk.media.cache.sourcestorage.SourceInfoStorage;
import tv.danmaku.ijk.media.cache.sourcestorage.SourceInfoStorageFactory;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static tv.danmaku.ijk.media.cache.Preconditions.checkNotNull;
import static tv.danmaku.ijk.media.cache.ProxyCacheUtils.DEFAULT_BUFFER_SIZE;

/**
 * Created by lin on 19-11-26.
 */
public class HttpUrlSource implements Source {

    private static final String TAG = "HttpUrlSource";

    private static final int MAX_REDIRECTS = 5;
    private final SourceInfoStorage sourceInfoStorage;
    private final HeaderInjector headerInjector;
    private SourceInfo sourceInfo;
    private Response response;
    private InputStream inputStream;

    public HttpUrlSource(String url) {
        this(url, SourceInfoStorageFactory.newEmptySourceInfoStorage());
    }

    public HttpUrlSource(String url, SourceInfoStorage sourceInfoStorage) {
        this(url, sourceInfoStorage, new EmptyHeadersInjector());
    }

    public HttpUrlSource(String url, SourceInfoStorage sourceInfoStorage, HeaderInjector headerInjector) {
        this.sourceInfoStorage = checkNotNull(sourceInfoStorage);
        this.headerInjector = checkNotNull(headerInjector);
        SourceInfo sourceInfo = sourceInfoStorage.get(url);
        this.sourceInfo = sourceInfo != null ? sourceInfo :
                new SourceInfo(url, Integer.MIN_VALUE, ProxyCacheUtils.getSupposablyMime(url));
    }

    public HttpUrlSource(HttpUrlSource source) {
        this.sourceInfo = source.sourceInfo;
        this.sourceInfoStorage = source.sourceInfoStorage;
        this.headerInjector = source.headerInjector;
    }

    @Override
    public synchronized long length() throws ProxyCacheException {
        if (sourceInfo.length == Integer.MIN_VALUE) {
            fetchContentInfo();
        }
        return sourceInfo.length;
    }

    @Override
    public void open(long offset) throws ProxyCacheException {
        try {
            response = openConnection(offset, -1);
            MediaType contentType = response.body().contentType();
            String mime = contentType == null ? null : contentType.toString();
            inputStream = new BufferedInputStream(response.body().byteStream(), DEFAULT_BUFFER_SIZE);
            long length = readSourceAvailableBytes(response, offset, response.code());
            this.sourceInfo = new SourceInfo(sourceInfo.url, length, mime);
            this.sourceInfoStorage.put(sourceInfo.url, sourceInfo);
        } catch (IOException e) {
            throw new ProxyCacheException("Error opening connection for " + sourceInfo.url + " with offset " + offset, e);
        }
    }

    private long readSourceAvailableBytes(Response response, long offset, int responseCode) throws IOException {
        long contentLength = getContentLength(response);
        return responseCode == HTTP_OK ? contentLength
                : responseCode == HTTP_PARTIAL ? contentLength + offset : sourceInfo.length;
    }

    private long getContentLength(Response response) {
        String contentLengthValue = response.header("Content-Length");
        return contentLengthValue == null ? -1 : Long.parseLong(contentLengthValue);
    }

    @Override
    public void close() throws ProxyCacheException {
        if (response != null) {
            try {
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
    }

    @Override
    public int read(byte[] buffer) throws ProxyCacheException {
        if (inputStream == null) {
            throw new ProxyCacheException("Error reading data from " + sourceInfo.url + ": connection is absent!");
        }
        try {
            return inputStream.read(buffer, 0, buffer.length);
        } catch (InterruptedIOException e) {
            throw new InterruptedProxyCacheException("Reading source " + sourceInfo.url + " is interrupted", e);
        } catch (IOException e) {
            throw new ProxyCacheException("Error reading data from " + sourceInfo.url, e);
        }
    }

    private void fetchContentInfo() throws ProxyCacheException {
        Log.d(TAG, "Read content info from " + sourceInfo.url);
        Response response = null;
        InputStream inputStream = null;
        try {
            response = openConnection(0, 10000);
            long length = getContentLength(response);
            MediaType contentType = response.body().contentType();
            String mime = contentType == null ? null : contentType.toString();
            inputStream = response.body().byteStream();
            this.sourceInfo = new SourceInfo(sourceInfo.url, length, mime);
            this.sourceInfoStorage.put(sourceInfo.url, sourceInfo);
            Log.d(TAG, "Source info fetched: " + sourceInfo);
        } catch (IOException e) {
            Log.e(TAG, "Error fetching info from " + sourceInfo.url, e);
        } finally {
            ProxyCacheUtils.close(inputStream);
            if (response != null) {
                response.close();
            }
        }
    }

    private Response openConnection(long offset, int timeout) throws IOException, ProxyCacheException {
        Response response;
        boolean redirected;
        int redirectCount = 0;
        String url = this.sourceInfo.url;
        do {
            Log.d(TAG, "Open connection " + (offset > 0 ? " with offset " + offset : "") + " to " + url);
            Request.Builder builder = new Request.Builder().url(url).get();
            injectCustomHeaders(builder, url);
            if (offset > 0) {
                builder.addHeader("Range", "bytes=" + offset + "-");
            }
            response = HttpUtil.getClient().newCall(builder.build()).execute();
            int code = response.code();
            redirected = code == HTTP_MOVED_PERM || code == HTTP_MOVED_TEMP || code == HTTP_SEE_OTHER;
            if (redirected) {
                url = response.header("Location");
                redirectCount++;
                response.close();
            }
            if (redirectCount > MAX_REDIRECTS) {
                throw new ProxyCacheException("Too many redirects: " + redirectCount);
            }
        } while (redirected);
        return response;
    }

    private void injectCustomHeaders(Request.Builder builder, String url) {
        Map<String, String> extraHeaders = headerInjector.addHeaders(url);
        for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
            builder.addHeader(header.getKey(), header.getValue());
        }
    }

    public synchronized String getMime() throws ProxyCacheException {
        if (TextUtils.isEmpty(sourceInfo.mime)) {
            fetchContentInfo();
        }
        return sourceInfo.mime;
    }

    public String getUrl() {
        return sourceInfo.url;
    }

    @Override
    public String toString() {
        return "HttpUrlSource{sourceInfo='" + sourceInfo + "}";
    }
}
