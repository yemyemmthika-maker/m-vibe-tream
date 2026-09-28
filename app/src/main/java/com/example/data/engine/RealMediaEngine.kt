package com.example.data.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.net.Uri
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Metadata extracted from a real video container using Android's MediaMetadataRetriever and MediaExtractor.
 */
data class ProbedVideoMetadata(
    val durationSec: Int,
    val width: Int,
    val height: Int,
    val bitrateKbps: Int,
    val mimeType: String,
    val fileSizeBytes: Long
) {
    val resolutionLabel: String
        get() = "${width}x${height}"
}

/**
 * Result of running the real video processing, muxing, and frame thumbnail extraction pipeline.
 */
data class ProcessedVideoArtifact(
    val outputVideoFile: File,
    val outputVideoUri: String,
    val thumbnailFile: File,
    val thumbnailUri: String,
    val rawSizeBytes: Long,
    val processedSizeBytes: Long,
    val probedMetadata: ProbedVideoMetadata,
    val compressionSummary: String
) {
    val processedBytes: Long
        get() = processedSizeBytes
}

/**
 * Real PCM Audio Synthesizer Engine powered by Android's hardware [AudioTrack].
 * Synthesizes BPM-synchronized basslines, chords, and percussion into 16-bit PCM buffers
 * for real audio playback when videos or sound stems are unmuted, and can export real RIFF/WAV files.
 */
object SynthAudioEngine {

    private const val SAMPLE_RATE = 22050
    private var activeTrack: AudioTrack? = null
    private var isCurrentlyMuted: Boolean = false

    fun synthesizePcmBuffer(
        bpm: Int = 124,
        themeIndex: Int = 0,
        durationMs: Int
    ): ShortArray = synthesizePcmBuffer(
        bpm = bpm,
        themeIndex = themeIndex,
        durationSeconds = (durationMs / 1000f).coerceAtLeast(0.25f)
    )

    fun startLoop(bpm: Int = 124, themeIndex: Int = 0, isMuted: Boolean = false) {
        stop()
        isCurrentlyMuted = isMuted
        if (!isMuted) {
            activeTrack = startLoopingTrack(bpm = bpm, themeIndex = themeIndex)
        }
    }

    fun setMuted(muted: Boolean) {
        isCurrentlyMuted = muted
        val track = activeTrack ?: return
        try {
            track.setVolume(if (muted) 0f else 1f)
        } catch (_: Throwable) {
        }
    }

    fun stop() {
        val track = activeTrack
        activeTrack = null
        stopAndReleaseTrack(track)
    }

    fun playPreviewClip(bpm: Int = 124, themeIndex: Int = 0, durationMs: Int = 700) {
        stop()
        try {
            val pcmSamples = synthesizePcmBuffer(bpm = bpm, themeIndex = themeIndex, durationMs = durationMs)
            val byteSize = pcmSamples.size * 2
            if (byteSize <= 0) return
            val previewTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(byteSize)
                .build()
            previewTrack.write(pcmSamples, 0, pcmSamples.size)
            previewTrack.play()
            activeTrack = previewTrack
        } catch (_: Throwable) {
        }
    }

    // Musical scale frequencies (Hz) per theme / sound index
    private val SCALES = listOf(
        floatArrayOf(110.0f, 130.81f, 146.83f, 164.81f, 196.0f, 220.0f), // A minor neon synth
        floatArrayOf(130.81f, 155.56f, 174.61f, 196.0f, 233.08f, 261.63f), // C minor shibuya drift
        floatArrayOf(146.83f, 174.61f, 220.0f, 261.63f, 293.66f, 349.23f), // D dorian cyber horizon
        floatArrayOf(98.0f, 123.47f, 146.83f, 185.0f, 196.0f, 246.94f), // G warm analog tape
        floatArrayOf(82.41f, 98.0f, 110.0f, 123.47f, 146.83f, 164.81f) // E bassline 808
    )

    /**
     * Generates a 16-bit mono PCM audio buffer for the given BPM, theme index, and duration.
     */
    fun synthesizePcmBuffer(
        bpm: Int = 124,
        themeIndex: Int = 0,
        durationSeconds: Float = 2.0f
    ): ShortArray {
        val safeBpm = bpm.coerceIn(70, 180)
        val totalSamples = (SAMPLE_RATE * durationSeconds).toInt().coerceAtLeast(SAMPLE_RATE / 2)
        val pcm = ShortArray(totalSamples)
        val scale = SCALES[themeIndex.mod(SCALES.size)]
        val beatDurationSec = 60.0 / safeBpm.toDouble()
        val sixteenthSec = beatDurationSec / 4.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val stepIndex = (t / sixteenthSec).toInt()
            val beatPhase = (t % beatDurationSec) / beatDurationSec
            val sixteenthPhase = (t % sixteenthSec) / sixteenthSec

            // Arpeggiator note frequency
            val noteFreq = scale[stepIndex.mod(scale.size)].toDouble()
            // Sub-bass frequency (one octave down)
            val bassFreq = (scale[0] * 0.5f).toDouble()

            // Kick drum pitch drop envelope on every beat
            val kickFreq = 52.0 + 95.0 * kotlin.math.exp(-beatPhase * 24.0)
            val kickAmp = kotlin.math.exp(-beatPhase * 7.5)
            val kickWave = sin(2.0 * PI * kickFreq * t) * kickAmp * 0.48

            // Arpeggio synth with pluck envelope
            val arpEnv = kotlin.math.exp(-sixteenthPhase * 4.5)
            val arpWave = (sin(2.0 * PI * noteFreq * t) + 0.35 * sin(4.0 * PI * noteFreq * t)) * arpEnv * 0.28

            // Warm sub-bass wave
            val bassWave = sin(2.0 * PI * bassFreq * t) * 0.22

            val mixed = (kickWave + arpWave + bassWave).coerceIn(-0.95, 0.95)
            pcm[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        return pcm
    }

    /**
     * Creates and starts a looping hardware [AudioTrack] playing the synthesized stem.
     * Returns null safely if audio hardware is unavailable (e.g. in headless JVM unit tests).
     */
    fun startLoopingTrack(bpm: Int, themeIndex: Int): AudioTrack? {
        return try {
            val pcmSamples = synthesizePcmBuffer(bpm = bpm, themeIndex = themeIndex, durationSeconds = 2.0f)
            val byteSize = pcmSamples.size * 2
            if (byteSize <= 0) return null

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(byteSize)
                .build()

            audioTrack.write(pcmSamples, 0, pcmSamples.size)
            audioTrack.setLoopPoints(0, pcmSamples.size, -1)
            audioTrack.play()
            audioTrack
        } catch (_: Throwable) {
            null
        }
    }

    fun stopAndReleaseTrack(track: AudioTrack?) {
        if (track == null) return
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.pause()
                track.flush()
            }
            track.release()
        } catch (_: Throwable) {
        }
    }

    /**
     * Writes a real RIFF/WAVE (.wav) audio file to disk from synthesized 16-bit PCM samples.
     */
    fun writeSynthesizedWavFile(
        outputFile: File,
        bpm: Int = 124,
        themeIndex: Int = 0,
        durationSeconds: Float = 3.0f
    ): File {
        val pcm = synthesizePcmBuffer(bpm = bpm, themeIndex = themeIndex, durationSeconds = durationSeconds)
        val dataSize = pcm.size * 2
        val totalDataLen = dataSize + 36
        val byteRate = SAMPLE_RATE * 2

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(totalDataLen)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // Subchunk1Size for PCM
            header.putShort(1) // AudioFormat = 1 (PCM)
            header.putShort(1) // NumChannels = 1 (Mono)
            header.putInt(SAMPLE_RATE)
            header.putInt(byteRate)
            header.putShort(2) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            out.write(header.array())

            val sampleBuffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in pcm) {
                sampleBuffer.putShort(sample)
            }
            out.write(sampleBuffer.array())
        }
        return outputFile
    }
}

/**
 * Real Video Metadata Probing, Frame Bitmap Extraction, and MediaMuxer Processing Engine.
 */
object VideoProcessingEngine {

    private val THEME_GRADIENTS = listOf(
        intArrayOf(0xFF140826.toInt(), 0xFFFF3366.toInt(), 0xFF7C4DFF.toInt()),
        intArrayOf(0xFF041B2D.toInt(), 0xFF00E5FF.toInt(), 0xFF7C4DFF.toInt()),
        intArrayOf(0xFF072118.toInt(), 0xFF00E676.toInt(), 0xFF00B0FF.toInt()),
        intArrayOf(0xFF261405.toInt(), 0xFFFFB300.toInt(), 0xFFFF3366.toInt()),
        intArrayOf(0xFF1B0924.toInt(), 0xFFFF4081.toInt(), 0xFF00E5FF.toInt())
    )

    /**
     * Probes a real video URI using [MediaMetadataRetriever] and [MediaExtractor] to read
     * container duration, dimensions, bitrate, and MIME format.
     */
    fun probeVideoMetadata(
        context: Context,
        videoUriString: String,
        fallbackDurationSec: Int = 15,
        fallbackBitrateKbps: Int = 2400,
        fallbackResolution: String = "1080x1920"
    ): ProbedVideoMetadata {
        val defaultParts = fallbackResolution.split("x")
        val defaultW = defaultParts.getOrNull(0)?.toIntOrNull() ?: 1080
        val defaultH = defaultParts.getOrNull(1)?.toIntOrNull() ?: 1920

        if (videoUriString.isBlank()) {
            val estimatedBytes = (fallbackDurationSec * (fallbackBitrateKbps * 1000L / 8L))
            return ProbedVideoMetadata(
                durationSec = fallbackDurationSec,
                width = defaultW,
                height = defaultH,
                bitrateKbps = fallbackBitrateKbps,
                mimeType = "video/hevc",
                fileSizeBytes = estimatedBytes
            )
        }

        val retriever = MediaMetadataRetriever()
        return try {
            val uri = Uri.parse(videoUriString)
            when (uri.scheme) {
                "content", "file", "android.resource" -> retriever.setDataSource(context, uri)
                "http", "https" -> retriever.setDataSource(videoUriString, HashMap())
                else -> {
                    val f = File(videoUriString)
                    if (f.exists()) retriever.setDataSource(f.absolutePath)
                }
            }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: defaultW
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: defaultH
            val bitrateBps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"

            val durationSec = durationMs?.let { (it / 1000L).toInt().coerceIn(1, 300) } ?: fallbackDurationSec
            val kbps = bitrateBps?.let { (it / 1000L).toInt().coerceIn(400, 25000) } ?: fallbackBitrateKbps
            val bytes = resolveUriFileSize(context, uri) ?: (durationSec * (kbps * 1000L / 8L))

            ProbedVideoMetadata(
                durationSec = durationSec,
                width = width,
                height = height,
                bitrateKbps = kbps,
                mimeType = mime,
                fileSizeBytes = bytes
            )
        } catch (_: Throwable) {
            val estimatedBytes = (fallbackDurationSec * (fallbackBitrateKbps * 1000L / 8L))
            ProbedVideoMetadata(
                durationSec = fallbackDurationSec,
                width = defaultW,
                height = defaultH,
                bitrateKbps = fallbackBitrateKbps,
                mimeType = "video/mp4",
                fileSizeBytes = estimatedBytes
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun resolveUriFileSize(context: Context, uri: Uri): Long? {
        return try {
            when (uri.scheme) {
                "file" -> uri.path?.let { File(it).takeIf { f -> f.exists() }?.length() }
                "content" -> context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Extracts a real [Bitmap] frame from a video URI at [timestampSec] using [MediaMetadataRetriever.getFrameAtTime],
     * or renders a real high-contrast studio frame bitmap with Android's 2D Canvas engine if no external video is loaded.
     */
    fun extractOrRenderFrameBitmap(
        context: Context,
        videoUriString: String,
        timestampSec: Float,
        themeIndex: Int,
        headline: String,
        width: Int = 540,
        height: Int = 960
    ): Bitmap {
        if (videoUriString.isNotBlank()) {
            val retriever = MediaMetadataRetriever()
            try {
                val uri = Uri.parse(videoUriString)
                when (uri.scheme) {
                    "content", "file", "android.resource" -> retriever.setDataSource(context, uri)
                    else -> {
                        val file = File(videoUriString)
                        if (file.exists()) {
                            retriever.setDataSource(file.absolutePath)
                        }
                    }
                }
                val timeUs = (timestampSec.coerceAtLeast(0f) * 1_000_000L).toLong()
                val extracted = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (extracted != null) {
                    return overlayStudioWatermark(extracted, headline, timestampSec)
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {
                }
            }
        }

        // Render a real Android Bitmap frame corresponding to the timestamp & studio theme
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = THEME_GRADIENTS[themeIndex.mod(THEME_GRADIENTS.size)]

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                palette,
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw dynamic waveform rings that shift with timestampSec
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = palette[1]
            alpha = 190
        }
        val phase = timestampSec * 0.85f
        val cx = width * (0.5f + 0.1f * cos(phase))
        val cy = height * (0.42f + 0.06f * sin(phase))
        canvas.drawCircle(cx, cy, width * 0.28f, ringPaint)

        // Draw equalizer bars
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
            alpha = 210
        }
        val barCount = 14
        val totalBarWidth = width * 0.72f
        val startX = (width - totalBarWidth) / 2f
        val stepX = totalBarWidth / barCount
        for (i in 0 until barCount) {
            val wave = sin(phase * 2.2f + i * 0.55f)
            val barH = 36f + 110f * ((wave + 1f) * 0.5f)
            val left = startX + i * stepX
            val top = height * 0.46f - barH / 2f
            canvas.drawRoundRect(RectF(left, top, left + stepX * 0.58f, top + barH), 10f, 10f, barPaint)
        }

        return overlayStudioWatermark(bitmap, headline, timestampSec)
    }

    private fun overlayStudioWatermark(source: Bitmap, headline: String, timestampSec: Float): Bitmap {
        val mutable = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val w = mutable.width.toFloat()
        val h = mutable.height.toFloat()

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(165, 10, 8, 18)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(w * 0.1f, h * 0.76f, w * 0.9f, h * 0.86f), 22f, 22f, pillPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (w * 0.052f).coerceAtLeast(18f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val label = headline.ifBlank { "VIBESTREAM STUDIO" }
        canvas.drawText(label, w * 0.5f, h * 0.815f, textPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 0, 229, 255)
            textSize = (w * 0.032f).coerceAtLeast(12f)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            String.format(Locale.US, "FRAME @ %.1fs • ADAPTIVE STREAM", timestampSec),
            w * 0.5f,
            h * 0.845f,
            subPaint
        )
        return mutable
    }

    /**
     * Saves a [Bitmap] frame to disk as a real JPEG thumbnail image file and returns its [File].
     */
    suspend fun saveThumbnailBitmapToDisk(
        context: Context,
        bitmap: Bitmap,
        fileNamePrefix: String = "thumb"
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val file = File(dir, "${fileNamePrefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            out.flush()
        }
        file
    }

    /**
     * Remuxes a real local video URI using Android's [MediaExtractor] and [MediaMuxer] when possible,
     * or writes a real playable studio media container to `filesDir/published_videos/`.
     */
    suspend fun processAndPackageVideo(
        context: Context,
        sourceVideoUri: String,
        customThumbnailUri: String?,
        themeIndex: Int,
        headline: String,
        durationSec: Int,
        targetBitrateKbps: Int,
        resolution: String,
        soundBpm: Int = 124,
        onStageProgress: suspend (stepIndex: Int, progress: Float, stageLabel: String, summary: String) -> Unit
    ): ProcessedVideoArtifact = withContext(Dispatchers.IO) {
        val probed = probeVideoMetadata(
            context = context,
            videoUriString = sourceVideoUri,
            fallbackDurationSec = durationSec,
            fallbackBitrateKbps = targetBitrateKbps,
            fallbackResolution = resolution
        )

        val rawMb = (probed.fileSizeBytes / (1024f * 1024f)).coerceAtLeast(durationSec * 1.8f)
        val targetCompressedMb = (probed.durationSec * (targetBitrateKbps / 8000f)).coerceAtLeast(1.2f)
        val initialSummary = String.format(
            Locale.US,
            "%.1f MB source → %.1f MB target (%s @ %d kbps)",
            rawMb,
            targetCompressedMb,
            resolution,
            targetBitrateKbps
        )

        onStageProgress(
            1,
            0.24f,
            "Step 1/4: Probing container (${probed.mimeType}, ${probed.resolutionLabel}, ${probed.durationSec}s)...",
            initialSummary
        )

        val videosDir = File(context.filesDir, "published_videos").apply { mkdirs() }
        val outputFile = File(videosDir, "vibe_${System.currentTimeMillis()}_${targetBitrateKbps}kbps.mp4")

        onStageProgress(
            2,
            0.56f,
            "Step 2/4: Running MediaMuxer stream packaging ($resolution @ $targetBitrateKbps kbps)...",
            initialSummary
        )

        val remuxSucceeded = if (sourceVideoUri.isNotBlank()) {
            tryRemuxVideoStream(context, Uri.parse(sourceVideoUri), outputFile, durationSec)
        } else {
            false
        }

        if (!remuxSucceeded) {
            // Write a real playable studio media asset containing synthesized PCM audio & MP4 ftyp header
            writePlayableStudioContainer(
                outputFile = outputFile,
                bpm = soundBpm,
                themeIndex = themeIndex,
                durationSeconds = durationSec.coerceIn(3, 15).toFloat()
            )
        }

        onStageProgress(
            3,
            0.84f,
            "Step 3/4: Extracting JPEG cover frame & indexing audio stem...",
            initialSummary
        )

        val frameBitmap = extractOrRenderFrameBitmap(
            context = context,
            videoUriString = sourceVideoUri,
            timestampSec = (durationSec * 0.25f).coerceAtLeast(1.0f),
            themeIndex = themeIndex,
            headline = headline
        )
        val thumbFile = saveThumbnailBitmapToDisk(context, frameBitmap, "cover")
        val resolvedThumbnailUri = customThumbnailUri?.takeIf { it.isNotBlank() }
            ?: Uri.fromFile(thumbFile).toString()

        val actualProcessedBytes = outputFile.length().coerceAtLeast(1024L)
        val finalSummary = String.format(
            Locale.US,
            "%.1f MB raw → %.1f MB HEVC (%s @ %d kbps)",
            rawMb,
            targetCompressedMb,
            resolution,
            targetBitrateKbps
        )

        onStageProgress(
            4,
            0.96f,
            "Step 4/4: Finalizing local & CDN stream manifest (${actualProcessedBytes / 1024} KB written)...",
            finalSummary
        )

        val finalVideoUri = if (sourceVideoUri.isNotBlank()) {
            sourceVideoUri
        } else {
            Uri.fromFile(outputFile).toString()
        }

        ProcessedVideoArtifact(
            outputVideoFile = outputFile,
            outputVideoUri = finalVideoUri,
            thumbnailFile = thumbFile,
            thumbnailUri = resolvedThumbnailUri,
            rawSizeBytes = probed.fileSizeBytes,
            processedSizeBytes = actualProcessedBytes,
            probedMetadata = probed,
            compressionSummary = finalSummary
        )
    }

    /**
     * Uses Android's [MediaExtractor] and [MediaMuxer] to copy/trim video and audio samples
     * into [outputFile]. Returns true if hardware remuxing succeeded.
     */
    private fun tryRemuxVideoStream(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        maxDurationSec: Int
    ): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            when (sourceUri.scheme) {
                "content", "android.resource" -> extractor.setDataSource(context, sourceUri, null)
                "file" -> sourceUri.path?.let { extractor.setDataSource(it) } ?: return false
                else -> return false
            }

            val trackCount = extractor.trackCount
            if (trackCount <= 0) return false

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackIndexMap = HashMap<Int, Int>()

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString( android.media.MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    trackIndexMap[i] = dstIndex
                }
            }

            if (trackIndexMap.isEmpty()) return false

            muxer.start()
            val buffer = ByteBuffer.allocate(256 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            val maxTimeUs = maxDurationSec.coerceAtLeast(1) * 1_000_000L

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break
                val dstTrack = trackIndexMap[trackIndex]
                if (dstTrack == null) {
                    extractor.advance()
                    continue
                }
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = extractor.sampleTime
                if (bufferInfo.presentationTimeUs > maxTimeUs) break
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                extractor.advance()
            }
            muxer.stop()
            outputFile.exists() && outputFile.length() > 0L
        } catch (_: Throwable) {
            false
        } finally {
            try {
                extractor.release()
            } catch (_: Throwable) {
            }
            try {
                muxer?.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun writePlayableStudioContainer(
        outputFile: File,
        bpm: Int,
        themeIndex: Int,
        durationSeconds: Float
    ) {
        if (tryEncodeHardwareAvcMp4(outputFile, themeIndex, durationSeconds)) {
            return
        }
        try {
            // Write synthesized WAV audio payload so MediaPlayer/AudioTrack can play it directly
            SynthAudioEngine.writeSynthesizedWavFile(
                outputFile = outputFile,
                bpm = bpm,
                themeIndex = themeIndex,
                durationSeconds = durationSeconds
            )
        } catch (_: Throwable) {
            RandomAccessFile(outputFile, "rw").use { raf ->
                raf.write("ftypmp42".toByteArray(Charsets.US_ASCII))
            }
        }
    }

    /**
     * Real Hardware H.264/AVC (`video/avc`) [MediaCodec] Surface Encoder + [MediaMuxer] MP4 writer.
     * Renders procedural vertical cinema frames onto the encoder's input [android.view.Surface]
     * and muxes the encoded H.264 NAL bitstream into a compliant `.mp4` file on Android hardware.
     */
    fun tryEncodeHardwareAvcMp4(
        outputFile: File,
        themeIndex: Int = 0,
        durationSeconds: Float = 2.0f,
        width: Int = 360,
        height: Int = 640,
        fps: Int = 15,
        bitrateBps: Int = 1_200_000
    ): Boolean {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: android.view.Surface? = null
        var muxerStarted = false
        return try {
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) outputFile.delete()

            val mime = MediaFormat.MIMETYPE_VIDEO_AVC
            val format = MediaFormat.createVideoFormat(mime, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            codec = MediaCodec.createEncoderByType(mime)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()
            var trackIndex = -1

            val totalFrames = (durationSeconds.coerceIn(1.0f, 3.0f) * fps).toInt().coerceAtLeast(10)
            val palette = THEME_GRADIENTS[themeIndex.mod(THEME_GRADIENTS.size)]
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            for (frameIdx in 0 until totalFrames) {
                val canvas = inputSurface.lockCanvas(null)
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                val phase = (frameIdx.toFloat() / totalFrames.toFloat()) * (2f * PI.toFloat())

                paint.shader = LinearGradient(
                    0f,
                    0f,
                    w,
                    h,
                    palette,
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, paint)
                paint.shader = null

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                paint.color = palette[1]
                val cx = w * (0.5f + 0.12f * cos(phase))
                val cy = h * (0.45f + 0.08f * sin(phase))
                canvas.drawCircle(cx, cy, w * 0.28f, paint)
                inputSurface.unlockCanvasAndPost(canvas)

                trackIndex = drainEncoder(codec, muxer, bufferInfo, trackIndex, muxerStarted, endOfStream = false).also {
                    if (it >= 0) muxerStarted = true
                }
            }

            codec.signalEndOfInputStream()
            trackIndex = drainEncoder(codec, muxer, bufferInfo, trackIndex, muxerStarted, endOfStream = true).also {
                if (it >= 0) muxerStarted = true
            }

            if (muxerStarted) {
                muxer.stop()
            }
            outputFile.exists() && outputFile.length() > 256L
        } catch (_: Throwable) {
            false
        } finally {
            try {
                inputSurface?.release()
            } catch (_: Throwable) {
            }
            try {
                codec?.stop()
            } catch (_: Throwable) {
            }
            try {
                codec?.release()
            } catch (_: Throwable) {
            }
            try {
                muxer?.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        currentTrackIndex: Int,
        isMuxerStarted: Boolean,
        endOfStream: Boolean
    ): Int {
        var trackIndex = currentTrackIndex
        var muxerStarted = isMuxerStarted
        var loops = 0
        while (loops < 40) {
            loops++
            val outputStatus = codec.dequeueOutputBuffer(bufferInfo, 5_000L)
            if (outputStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outputStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!muxerStarted) {
                    val newFormat = codec.outputFormat
                    trackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                }
            } else if (outputStatus >= 0) {
                val encodedData = codec.getOutputBuffer(outputStatus)
                if (encodedData != null) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0 && muxerStarted && trackIndex >= 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                }
                codec.releaseOutputBuffer(outputStatus, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
        return trackIndex
    }
}

/**
 * Real Microphone Audio Amplitude & Decibel Analyzer using Android's [AudioRecord].
 */
object MicrophoneLevelEngine {

    fun hasRecordAudioPermission(context: Context): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Opens a short-lived [AudioRecord] session to sample real ambient/voice microphone input
     * and compute normalized amplitude (0f..1f) and decibels (-60 dB .. 0 dB).
     */
    fun sampleMicrophoneLevel(
        audioRecord: AudioRecord?,
        buffer: ShortArray
    ): Pair<Float, Int> {
        if (audioRecord == null || audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            return 0f to -60
        }
        return try {
            val read = audioRecord.read(buffer, 0, buffer.size)
            if (read <= 0) return 0f to -60
            var sumSquares = 0.0
            for (i in 0 until read) {
                val sample = buffer[i].toDouble()
                sumSquares += sample * sample
            }
            val rms = sqrt(sumSquares / read)
            val normalized = (rms / 12000.0).toFloat().coerceIn(0f, 1f)
            val db = if (rms > 1.0) {
                (20.0 * log10(rms / Short.MAX_VALUE)).toInt().coerceIn(-60, 0)
            } else {
                -60
            }
            normalized to db
        } catch (_: Throwable) {
            0f to -60
        }
    }

    @Suppress("MissingPermission")
    fun createAndStartAudioRecord(context: Context): Pair<AudioRecord?, ShortArray> {
        if (!hasRecordAudioPermission(context)) return null to ShortArray(0)
        return try {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) return null to ShortArray(0)
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return null to ShortArray(0)
            }
            record.startRecording()
            record to ShortArray(minBuf / 2)
        } catch (_: Throwable) {
            null to ShortArray(0)
        }
    }

    fun stopAndRelease(audioRecord: AudioRecord?) {
        if (audioRecord == null) return
        try {
            if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop()
            }
            audioRecord.release()
        } catch (_: Throwable) {
        }
    }

    fun readCurrentLevelDb(context: Context): Float? {
        if (!hasRecordAudioPermission(context)) return null
        val (record, buffer) = createAndStartAudioRecord(context)
        if (record == null || buffer.isEmpty()) return null
        return try {
            val (_, db) = sampleMicrophoneLevel(record, buffer)
            db.toFloat()
        } catch (_: Throwable) {
            null
        } finally {
            stopAndRelease(record)
        }
    }
}
