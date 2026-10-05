package com.example.tsuyu

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import java.nio.LongBuffer

class OnnxOcrEngine(private val context: Context) : AutoCloseable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val encoderSession: OrtSession
    private val decoderSession: OrtSession
    private val tokenizer: JapaneseTokenizer = JapaneseTokenizer(context)

    init {
        val encoderBytes = context.assets.open("encoder_model_quantized.onnx").use { it.readBytes() }
        val decoderBytes = context.assets.open("decoder_model_quantized.onnx").use { it.readBytes() }

        val sessionOptions = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
        }

        encoderSession = env.createSession(encoderBytes, sessionOptions)
        decoderSession = env.createSession(decoderBytes, sessionOptions)
    }

    suspend fun extractText(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val startTotal = System.currentTimeMillis()

        val encoderInputName = encoderSession.inputNames.iterator().next()
        val decoderInputNames = decoderSession.inputNames.toList()

        val inputIdsName = decoderInputNames.find { it.contains("input_ids") } 
            ?: decoderInputNames.first()
        val encoderHiddenStateName = decoderInputNames.find { it.contains("encoder") || it.contains("hidden") } 
            ?: decoderInputNames.elementAtOrElse(1) { "encoder_hidden_states" }

        // 1. Preprocess bitmap to 1x3x224x224 normalized float tensor
        val startPrep = System.currentTimeMillis()
        val imageTensor = prepareImageTensor(bitmap)
        val prepTime = System.currentTimeMillis() - startPrep

        val generatedTokens = mutableListOf<Long>(JapaneseTokenizer.CLS_TOKEN_ID)

        val startInference = System.currentTimeMillis()
        try {
            // 2. Encoder pass
            encoderSession.run(mapOf(encoderInputName to imageTensor)).use { encResult ->
                val encoderHiddenStateValue = encResult.get(0)
                val encoderHiddenStateTensor = encoderHiddenStateValue as OnnxTensor

                // 3. Autoregressive Decoder Loop
                val maxLen = 300

                for (step in 0 until maxLen) {
                    val seqLen = generatedTokens.size.toLong()
                    val inputIdsBuffer = LongBuffer.allocate(generatedTokens.size)
                    generatedTokens.forEach { inputIdsBuffer.put(it) }
                    inputIdsBuffer.rewind()

                    val inputIdsTensor = OnnxTensor.createTensor(env, inputIdsBuffer, longArrayOf(1, seqLen))

                    try {
                        val decoderInputs = mapOf(
                            inputIdsName to inputIdsTensor,
                            encoderHiddenStateName to encoderHiddenStateTensor
                        )

                        decoderSession.run(decoderInputs).use { decResult ->
                            val logitsTensor = decResult.get(0) as OnnxTensor
                            val logitsInfo = logitsTensor.info as TensorInfo
                            val vocabSize = logitsInfo.shape[2].toInt()
                            val lastStepOffset = (seqLen.toInt() - 1) * vocabSize

                            val floatBuffer = logitsTensor.floatBuffer

                            var maxLogit = -Float.MAX_VALUE
                            var maxTokenId = JapaneseTokenizer.UNK_TOKEN_ID

                            for (v in 0 until vocabSize) {
                                val logit = floatBuffer.get(lastStepOffset + v)
                                if (logit > maxLogit) {
                                    maxLogit = logit
                                    maxTokenId = v.toLong()
                                }
                            }

                            if (maxTokenId == JapaneseTokenizer.SEP_TOKEN_ID) {
                                break // Exit autoregressive loop
                            }

                            generatedTokens.add(maxTokenId)
                        }
                    } finally {
                        inputIdsTensor.close()
                    }

                    if (generatedTokens.last() == JapaneseTokenizer.SEP_TOKEN_ID) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            imageTensor.close()
        }

        val inferenceTime = System.currentTimeMillis() - startInference
        val totalTime = System.currentTimeMillis() - startTotal
        android.util.Log.d("TsuyuBenchmark", "Preprocessing: ${prepTime}ms | Inference: ${inferenceTime}ms | Total: ${totalTime}ms")

        // 4. Decode generated token IDs into Japanese text
        tokenizer.decode(generatedTokens)
    }

    private fun prepareImageTensor(bitmap: Bitmap): OnnxTensor {
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val pixels = IntArray(224 * 224)
        resized.getPixels(pixels, 0, 224, 0, 0, 224, 224)
        if (resized != bitmap) {
            resized.recycle()
        }
        // Explicitly recycle the source bitmap as soon as pixels are extracted
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }

        val floatBuffer = FloatBuffer.allocate(1 * 3 * 224 * 224)
        val area = 224 * 224

        // Mean = 0.5, Std = 0.5 -> (value / 255.0 - 0.5) / 0.5 = value / 127.5 - 1.0
        for (i in 0 until area) {
            val pixel = pixels[i]
            val r = (pixel shr 16 and 0xFF) / 127.5f - 1.0f
            val g = (pixel shr 8 and 0xFF) / 127.5f - 1.0f
            val b = (pixel and 0xFF) / 127.5f - 1.0f

            floatBuffer.put(i, r)
            floatBuffer.put(area + i, g)
            floatBuffer.put(2 * area + i, b)
        }
        floatBuffer.rewind()

        return OnnxTensor.createTensor(env, floatBuffer, longArrayOf(1, 3, 224, 224))
    }

    override fun close() {
        encoderSession.close()
        decoderSession.close()
        env.close()
    }
}
