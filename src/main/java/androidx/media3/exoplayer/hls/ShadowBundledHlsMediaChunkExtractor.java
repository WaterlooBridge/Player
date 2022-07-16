package androidx.media3.exoplayer.hls;

import androidx.annotation.VisibleForTesting;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.mp3.Mp3Extractor;
import androidx.media3.extractor.mp4.FragmentedMp4Extractor;
import androidx.media3.extractor.ts.Ac3Extractor;
import androidx.media3.extractor.ts.Ac4Extractor;
import androidx.media3.extractor.ts.AdtsExtractor;
import androidx.media3.extractor.ts.ShadowTsExtractor;
import androidx.media3.extractor.ts.TsExtractor;
import java.io.IOException;

/**
 * {@link HlsMediaChunkExtractor} implementation that uses ExoPlayer app-bundled {@link Extractor
 * Extractors}.
 */
@UnstableApi
public final class ShadowBundledHlsMediaChunkExtractor implements HlsMediaChunkExtractor {

    private static final PositionHolder POSITION_HOLDER = new PositionHolder();

    @VisibleForTesting /* package */ final Extractor extractor;
    private final Format multivariantPlaylistFormat;
    private final TimestampAdjuster timestampAdjuster;

    /**
     * Creates a new instance.
     *
     * @param extractor The underlying {@link Extractor}.
     * @param multivariantPlaylistFormat The {@link Format} obtained from the multivariant playlist.
     * @param timestampAdjuster A {@link TimestampAdjuster} to adjust sample timestamps.
     */
    public ShadowBundledHlsMediaChunkExtractor(
            Extractor extractor, Format multivariantPlaylistFormat, TimestampAdjuster timestampAdjuster) {
        this.extractor = extractor;
        this.multivariantPlaylistFormat = multivariantPlaylistFormat;
        this.timestampAdjuster = timestampAdjuster;
    }

    @Override
    public void init(ExtractorOutput extractorOutput) {
        extractor.init(extractorOutput);
    }

    @Override
    public boolean read(ExtractorInput extractorInput) throws IOException {
        return extractor.read(extractorInput, POSITION_HOLDER) == Extractor.RESULT_CONTINUE;
    }

    @Override
    public boolean isPackedAudioExtractor() {
        return extractor instanceof AdtsExtractor
                || extractor instanceof Ac3Extractor
                || extractor instanceof Ac4Extractor
                || extractor instanceof Mp3Extractor;
    }

    @Override
    public boolean isReusable() {
        return extractor instanceof ShadowTsExtractor || extractor instanceof TsExtractor || extractor instanceof FragmentedMp4Extractor;
    }

    @Override
    public HlsMediaChunkExtractor recreate() {
        Assertions.checkState(!isReusable());
        Extractor newExtractorInstance;
        if (extractor instanceof WebvttExtractor) {
            newExtractorInstance =
                    new WebvttExtractor(multivariantPlaylistFormat.language, timestampAdjuster);
        } else if (extractor instanceof AdtsExtractor) {
            newExtractorInstance = new AdtsExtractor();
        } else if (extractor instanceof Ac3Extractor) {
            newExtractorInstance = new Ac3Extractor();
        } else if (extractor instanceof Ac4Extractor) {
            newExtractorInstance = new Ac4Extractor();
        } else if (extractor instanceof Mp3Extractor) {
            newExtractorInstance = new Mp3Extractor();
        } else {
            throw new IllegalStateException(
                    "Unexpected extractor type for recreation: " + extractor.getClass().getSimpleName());
        }
        return new BundledHlsMediaChunkExtractor(
                newExtractorInstance, multivariantPlaylistFormat, timestampAdjuster);
    }

    @Override
    public void onTruncatedSegmentParsed() {
        extractor.seek(/* position= */ 0, /* timeUs= */ 0);
    }
}
