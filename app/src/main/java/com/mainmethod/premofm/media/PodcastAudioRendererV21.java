package com.mainmethod.premofm.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.util.Util;

import java.nio.ByteBuffer;

/**
 * Custom renderer used for PremoFM
 * Created by evanhalley on 12/4/15.
 */
public class PodcastAudioRendererV21 extends PodcastAudioRenderer {

    private static final int SAMPLES_PER_CODEC_FRAME = 1_024;

    private Sonic mSonic;
    private byte[] mSonicInputBuffer;
    private byte[] mSonicOutputBuffer;

    private float mSpeed = 1.0f;
    private int mLastSeenBufferIndex = -1;
    private ByteBuffer mLastInternalBuffer;

    public PodcastAudioRendererV21(SampleSource source) {
        super(source);
    }

    public synchronized void setSpeed(float speed) {
        this.mSpeed = speed;

        if (mSonic != null) {
            this.mSonic.setSpeed(speed);
        }
    }

    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec,
                                          ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo, int bufferIndex,
                                          boolean shouldSkip) throws ExoPlaybackException {

        if (bufferIndex == mLastSeenBufferIndex) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec,
                    mLastInternalBuffer, bufferInfo, bufferIndex, shouldSkip);
        } else {
            mLastSeenBufferIndex = bufferIndex;
        }
        final int bytesToRead;

        if (Util.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            buffer.position(0);
            bytesToRead = bufferInfo.size;
        } else {
            bytesToRead = buffer.remaining();
        }

        buffer.get(mSonicInputBuffer, 0, bytesToRead);
        mSonic.writeBytesToStream(mSonicInputBuffer, bytesToRead);
        final int readThisTime = mSonic.readBytesFromStream(mSonicOutputBuffer, mSonicOutputBuffer.length);

        bufferInfo.offset = 0;
        mLastInternalBuffer.position(0);

        bufferInfo.size = readThisTime;
        mLastInternalBuffer.limit(readThisTime);

        return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, mLastInternalBuffer,
                bufferInfo, bufferIndex, shouldSkip);
    }

    @Override
    protected void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
        super.onOutputFormatChanged(codec, format);

        final int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        final int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        // Two samples per frame * 2 to support narration speeds down to 0.5
        final int bufferSizeBytes = SAMPLES_PER_CODEC_FRAME * 2 * 2 * channelCount;

        this.mSonicInputBuffer = new byte[bufferSizeBytes];
        this.mSonicOutputBuffer = new byte[bufferSizeBytes];
        this.mSonic = new Sonic(sampleRate, channelCount);
        this.mLastInternalBuffer = ByteBuffer.wrap(mSonicOutputBuffer, 0, 0);
        setSpeed(mSpeed);
    }
}