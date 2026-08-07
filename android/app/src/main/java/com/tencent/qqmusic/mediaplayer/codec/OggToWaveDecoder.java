package com.tencent.qqmusic.mediaplayer.codec;

import com.tencent.qqmusic.mediaplayer.AudioInformation;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Resource conversion stage used before the real-time synthesis engine starts. */
public final class OggToWaveDecoder {
    private static final int BUFFER_SIZE = 1024;

    private OggToWaveDecoder() {}

    public static boolean decode(File source, File target) {
        File temporary = new File(target.getParentFile(), target.getName() + ".part");
        if (temporary.exists() && !temporary.delete()) return false;

        NativeDecoder decoder = new NativeDecoder();
        try (RandomAccessFile output = new RandomAccessFile(temporary, "rw")) {
            if (decoder.init(source.getAbsolutePath(), false) != 0) return false;
            AudioInformation information = decoder.getAudioInformation();
            if (information == null) return false;

            int sampleRate = (int) information.getSampleRate();
            int channels = information.getChannels();
            int bytesPerSample = information.getBitDepth();
            if (sampleRate <= 0 || channels <= 0) return false;
            if (bytesPerSample <= 0) bytesPerSample = 2;

            output.setLength(0);
            output.write(new byte[44]);
            byte[] buffer = new byte[BUFFER_SIZE];
            long dataSize = 0;
            while (true) {
                int decoded = decoder.decodeData(buffer.length, buffer);
                if (decoded <= 0) break;
                output.write(buffer, 0, decoded);
                dataSize += decoded;
            }

            int frameSize = channels * bytesPerSample;
            while (frameSize > 0 && dataSize % frameSize != 0) {
                output.write(0);
                dataSize++;
            }
            writeWaveHeader(output, dataSize, sampleRate, channels, bytesPerSample * 8);
        } catch (Throwable error) {
            temporary.delete();
            return false;
        } finally {
            decoder.release();
        }

        if (target.exists() && !target.delete()) {
            temporary.delete();
            return false;
        }
        return temporary.renameTo(target);
    }

    private static void writeWaveHeader(
            RandomAccessFile output,
            long dataSize,
            int sampleRate,
            int channels,
            int bitDepth
    ) throws IOException {
        long byteRate = (long) sampleRate * channels * bitDepth / 8;
        int blockAlign = channels * bitDepth / 8;
        output.seek(0);
        output.writeBytes("RIFF");
        writeIntLE(output, dataSize + 36);
        output.writeBytes("WAVEfmt ");
        writeIntLE(output, 16);
        writeShortLE(output, 1);
        writeShortLE(output, channels);
        writeIntLE(output, sampleRate);
        writeIntLE(output, byteRate);
        writeShortLE(output, blockAlign);
        writeShortLE(output, bitDepth);
        output.writeBytes("data");
        writeIntLE(output, dataSize);
    }

    private static void writeIntLE(RandomAccessFile output, long value) throws IOException {
        output.write((int) value & 0xff);
        output.write((int) (value >> 8) & 0xff);
        output.write((int) (value >> 16) & 0xff);
        output.write((int) (value >> 24) & 0xff);
    }

    private static void writeShortLE(RandomAccessFile output, int value) throws IOException {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
    }
}
