package com.example.smartwindowsill

import android.util.Base64
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.TextBlockParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Ideal growing conditions for a plant, as estimated by Claude
data class PlantCareInfo(
    val minTemp: Float, val optTemp: Float, val maxTemp: Float,         // °C
    val minHumidity: Float, val optHumidity: Float, val maxHumidity: Float, // %
    val minLight: Float, val optLight: Float, val maxLight: Float,       // lx
    val scientificName: String? = null                                  // Научное/латинское название
)

object PlantCareClient {

    private const val MODEL = "claude-haiku-5-5"

    private val client: AnthropicClient by lazy {
        AnthropicOkHttpClient.builder()
            .apiKey(BuildConfig.ANTHROPIC_API_KEY)
            .build()
    }

    suspend fun fetchCareInfo(plantName: String, photoJpeg: ByteArray): PlantCareInfo =
        withContext(Dispatchers.IO) {
            require(BuildConfig.ANTHROPIC_API_KEY.isNotBlank()) {
                "Не задан anthropic.apiKey в local.properties"
            }

            val prompt = """
                The attached photo shows a houseplant. Its name is "$plantName".
                Identify its scientific/botanical Latin binomial name (e.g. "Monstera deliciosa") and
                estimate its ideal growing conditions for an indoor windowsill.
                Respond with ONLY a JSON object, no other text, with these keys:
                "scientificName": string (Latin botanical name, e.g. "Monstera deliciosa"),
                "minTemp", "optTemp", "maxTemp": numbers (degrees Celsius),
                "minHumidity", "optHumidity", "maxHumidity": numbers (relative humidity, percent),
                "minLight", "optLight", "maxLight": numbers (illuminance, lux).
                Ensure min <= opt <= max for each group.
            """.trimIndent()

            val params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(1024L)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .addUserMessageOfBlockParams(
                    listOf(
                        ContentBlockParam.ofImage(
                            ImageBlockParam.builder()
                                .source(
                                    Base64ImageSource.builder()
                                        .data(Base64.encodeToString(photoJpeg, Base64.NO_WRAP))
                                        .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                                        .build()
                                )
                                .build()
                        ),
                        ContentBlockParam.ofText(TextBlockParam.builder().text(prompt).build())
                    )
                )
                .build()

            val text = client.messages().create(params).content()
                .mapNotNull { block -> block.text().orElse(null)?.text() }
                .joinToString("")

            parseCareInfo(text)
        }

    private fun parseCareInfo(text: String): PlantCareInfo {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        require(start >= 0 && end > start) { "Claude не вернул JSON" }

        val json = JSONObject(text.substring(start, end + 1))
        fun num(key: String) = json.getDouble(key).toFloat()
        val sciName = json.optString("scientificName", "").trim().takeIf { it.isNotEmpty() }

        val info = PlantCareInfo(
            minTemp = num("minTemp"), optTemp = num("optTemp"), maxTemp = num("maxTemp"),
            minHumidity = num("minHumidity"), optHumidity = num("optHumidity"), maxHumidity = num("maxHumidity"),
            minLight = num("minLight"), optLight = num("optLight"), maxLight = num("maxLight"),
            scientificName = sciName
        )

        require(info.minTemp <= info.optTemp && info.optTemp <= info.maxTemp) { "Неверные температуры от Claude" }
        require(info.minHumidity <= info.optHumidity && info.optHumidity <= info.maxHumidity) { "Неверная влажность от Claude" }
        require(info.minLight <= info.optLight && info.optLight <= info.maxLight) { "Неверная освещенность от Claude" }
        return info
    }
}
