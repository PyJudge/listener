package com.listener.domain.usecase

import android.content.Context
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * AudioPreprocessUseCase 압축 테스트
 *
 * 실행: ./gradlew connectedAndroidTest --tests "*.AudioPreprocessUseCaseTest"
 */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class AudioPreprocessUseCaseTest {

    private lateinit var context: Context
    private lateinit var useCase: AudioPreprocessUseCase

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        useCase = AudioPreprocessUseCase(context)
    }

    @Test
    fun testAudioCompression() = runBlocking {
        // 테스트용 WAV 파일 생성 (무음, 10초, 44.1kHz stereo)
        val testFile = createTestWavFile()
        val originalSize = testFile.length()
        Log.i("AudioPreprocessTest", "Original size: ${originalSize / 1024}KB")

        // 압축 실행
        val result = useCase.preprocess(testFile)

        // 결과 확인
        val processedFile = File(result.chunks.first().path)
        val compressedSize = processedFile.length()
        Log.i("AudioPreprocessTest", "Compressed size: ${compressedSize / 1024}KB")
        Log.i("AudioPreprocessTest", "Compression ratio: ${(1 - compressedSize.toFloat() / originalSize) * 100}%")

        // 압축 확인 - 최소 50% 이상 줄어야 함
        assertTrue(
            "압축이 충분하지 않음: ${compressedSize}B (원본: ${originalSize}B)",
            compressedSize < originalSize * 0.5
        )

        // 정리
        testFile.delete()
        useCase.cleanupTempFiles()
    }

    /**
     * 테스트용 WAV 파일 생성 (무음, 10초)
     * WAV 형식: 44.1kHz, 16-bit, Stereo
     */
    private fun createTestWavFile(): File {
        val sampleRate = 44100
        val channels = 2
        val bitsPerSample = 16
        val durationSeconds = 10

        val numSamples = sampleRate * durationSeconds * channels
        val dataSize = numSamples * (bitsPerSample / 8)
        val fileSize = 44 + dataSize // WAV header (44 bytes) + data

        val file = File(context.cacheDir, "test_input.wav")
        file.outputStream().use { out ->
            // RIFF header
            out.write("RIFF".toByteArray())
            out.write(intToBytes(fileSize - 8, 4))
            out.write("WAVE".toByteArray())

            // fmt chunk
            out.write("fmt ".toByteArray())
            out.write(intToBytes(16, 4)) // chunk size
            out.write(intToBytes(1, 2)) // audio format (PCM)
            out.write(intToBytes(channels, 2))
            out.write(intToBytes(sampleRate, 4))
            out.write(intToBytes(sampleRate * channels * bitsPerSample / 8, 4)) // byte rate
            out.write(intToBytes(channels * bitsPerSample / 8, 2)) // block align
            out.write(intToBytes(bitsPerSample, 2))

            // data chunk
            out.write("data".toByteArray())
            out.write(intToBytes(dataSize, 4))

            // 무음 데이터 (0으로 채움)
            val buffer = ByteArray(8192)
            var remaining = dataSize
            while (remaining > 0) {
                val toWrite = minOf(buffer.size, remaining)
                out.write(buffer, 0, toWrite)
                remaining -= toWrite
            }
        }

        Log.i("AudioPreprocessTest", "Created test WAV: ${file.length() / 1024}KB")
        return file
    }

    private fun intToBytes(value: Int, size: Int): ByteArray {
        val bytes = ByteArray(size)
        for (i in 0 until size) {
            bytes[i] = ((value shr (8 * i)) and 0xFF).toByte()
        }
        return bytes
    }
}
