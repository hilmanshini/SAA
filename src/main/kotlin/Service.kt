//import retrofit2.Call
//import retrofit2.http.Body
//import retrofit2.http.POST
//import retrofit2.http.Query
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import java.util.concurrent.TimeUnit
//
//
//// Request body structure
//data class GeminiRequest(
//    val contents: List<ContentRequest>
//)
//
//data class ContentRequest(
//    val parts: List<PartRequest>
//)
//
//data class PartRequest(
//    val text: String
//)
//
//// Response body structure (adapt based on actual response)
//data class GeminiResponse(
//    val candidates: List<Candidate>,
//    val usageMetadata: UsageMetadata,
//    val modelVersion: String
//)
//
//data class Candidate(
//    val content: Content,
//    val finishReason: String,
//    val avgLogprobs: Double
//)
//
//data class Content(
//    val parts: List<Part>,
//    val role: String
//)
//
//data class Part(
//    val text: String
//)
//
//data class UsageMetadata(
//    val promptTokenCount: Int,
//    val candidatesTokenCount: Int,
//    val totalTokenCount: Int,
//    val promptTokensDetails: List<TokenDetail>,
//    val candidatesTokensDetails: List<TokenDetail>
//)
//
//data class TokenDetail(
//    val modality: String,
//    val tokenCount: Int
//)
//
//
//
//
//interface GeminiApiService {
//    @POST("v1beta/models/gemini-2.0-flash:generateContent")
//    fun generateContent(
//        @Query("key") apiKey: String,  // Add the API key as a query parameter
//        @Body request: GeminiRequest   // Send the request body
//    ): Call<GeminiResponse>
//}
//
//
//
//fun createRetrofitClient(): Retrofit {
//    val loggingInterceptor = HttpLoggingInterceptor().apply {
//        level = HttpLoggingInterceptor.Level.BODY
//    }
//
//    val okHttpClient = OkHttpClient.Builder()
//        .connectTimeout(60, TimeUnit.SECONDS)  // connection timeout
//        .readTimeout(60, TimeUnit.SECONDS)     // socket read timeout
//        .writeTimeout(60, TimeUnit.SECONDS)    // socket write timeout
//        .addInterceptor(loggingInterceptor)
//        .build()
//
//    return Retrofit.Builder()
//        .baseUrl("https://generativelanguage.googleapis.com/") // Base URL
//        .client(okHttpClient)
//        .addConverterFactory(GsonConverterFactory.create())
//        .build()
//}
//
//val geminiApiService by lazy {
//    val retrofit = createRetrofitClient()
//
//    // Create an instance of the API service
//    val geminiApiService = retrofit.create(GeminiApiService::class.java)
//
//    // Set the API key (replace with your actual key)
//
//    geminiApiService
//}
//val apiKey = "AIzaSyATuBQmrEzKmeigyHw2r2OvHCK6qCV7AsM"
//
//class  AiService{
//    fun  response(message: String): String{
//
//        // Build the request payload
//        val request = GeminiRequest(
//            contents = listOf(
//                ContentRequest(
//                    parts = listOf(PartRequest(text = message))
//                )
//            )
//        )
//
//        // Make the API call asynchronously
//        val call = geminiApiService.generateContent(apiKey, request)
//
//        // Execute the request
//        val response = call.execute()
//
//        if (response.isSuccessful) {
//            val responseBody = response.body()
//            return ("${responseBody?.candidates?.first()?.content?.parts?.first()?.text}")
//        } else {
//            return ("Error: ${response.message()}")
//        }
//    }
//}
