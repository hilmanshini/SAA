import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// Request Data Models
data class GeminiRequest(val contents: List<ContentRequest>)

data class ContentRequest(val parts: List<PartRequest>)

data class PartRequest(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String
)

// Response Data Models (simplified)
data class GeminiResponse(
    val candidates: List<Candidate>
)

data class Candidate(
    val content: Content
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

// API Interface
interface GeminiApiService {
    @POST("v1/models/gemini-2.5-flash:generateContent")
    fun generateContentWithImage(
        @Query("key") apiKey: String = "AIzaSyATuBQmrEzKmeigyHw2r2OvHCK6qCV7AsM",
        @Body request: GeminiRequest
    ): Call<GeminiResponse>
}
//
//// Client Factory
//object GeminiClient {
//    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
//    private const val TIMEOUT_SECONDS = 6000L
//
//    fun create(): GeminiApiService {
//        val logging = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }
//
//        val client = OkHttpClient.Builder()
//            .addInterceptor(logging)
//            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
//            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
//            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
//            .build()
//
//        val retrofit = Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .client(client)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        return retrofit.create(GeminiApiService::class.java)
//    }
//    class Caller{
//
//        companion object {
//            fun encodeImageToBase64(filePath: String): String {
//                val file = File(filePath)
//                val bytes = file.readBytes()
//                return Base64.getEncoder().encodeToString(bytes)
//            }
//
//            fun buildVisionRequest(prompt: String, imagePath: String): GeminiRequest {
//                val base64Image = encodeImageToBase64(imagePath)
//
//                return GeminiRequest(
//                    contents = listOf(
//                        ContentRequest(
//                            parts = listOf(
//                                PartRequest(text = prompt),
//                                PartRequest(
//                                    inlineData = InlineData(
//                                        mimeType = "image/png", // or "image/jpeg"
//                                        data = base64Image
//                                    )
//                                )
//                            )
//                        )
//                    )
//                )
//            }
//
//            fun sendImageToGemini(imagePath: String, prompt: String = "Please solve this, only the solution, not anything else , use Java as programming language"): String {
//                val service = create()
//                val request = buildVisionRequest(prompt, imagePath)
//                val response = service.generateContentWithImage(request = request).execute()
//
//
//                val responseText =  if (response.isSuccessful) {
//                    response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No result"
//                } else {
//                    "Error: ${response.code()} ${response.message()}"
//                }
//                println(responseText)
//                val file  = File("${Constants.UserHome}/gpt_reponse/${SimpleDateFormat("yyyy-MM-dd").format(Date())}${UUID.randomUUID()}").apply {
//                    mkdirs()
//                }
//                val image = File(file.absolutePath,"question.png")
//                val responseTextFile = File(file.absolutePath,"response.txt")
//                val dest  = Paths.get(image.absolutePath)
//                val source = Paths.get(imagePath)
//                Files.copy(source,dest, StandardCopyOption.REPLACE_EXISTING)
//                responseTextFile.writeText(responseText)
//                print("saved to ${file.absolutePath}")
//                return  responseText;
//            }
//        }
//
//    }
//}