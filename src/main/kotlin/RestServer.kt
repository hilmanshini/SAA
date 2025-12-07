import io.ktor.http.content.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdfwriter.compress.CompressParameters
import java.io.File
import java.io.IOException

class RestServer(val floatingWindow: FloatingWindow) {
    
    /**
     * Formats file size in human-readable format
     */
    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.2f MB", mb)
            kb >= 1.0 -> String.format("%.2f KB", kb)
            else -> "$bytes bytes"
        }
    }
    
    /**
     * Compresses a PDF file to reduce its size while maintaining readability
     * Uses PDFBox to optimize the PDF structure and compress content streams
     * For image-based or corrupted PDFs that can't be loaded, falls back to using original file
     * @param compressionQuality Not directly used by PDFBox, but kept for API compatibility
     */
    private suspend fun compressPdfFile(inputFile: File, outputFile: File, compressionQuality: Float = 0.5f) {
        withContext(Dispatchers.IO) {
            // Validate input file exists and has content
            if (!inputFile.exists() || inputFile.length() == 0L) {
                println("Error: Input file does not exist or is empty")
                if (inputFile.exists()) {
                    inputFile.copyTo(outputFile, overwrite = true)
                }
                return@withContext
            }
            
            // Quick validation: check if file starts with PDF header
            try {
                val firstBytes = inputFile.inputStream().use { it.readNBytes(4) }
                if (!firstBytes.contentEquals("%PDF".toByteArray())) {
                    println("Warning: File doesn't appear to be a valid PDF, skipping compression")
                    inputFile.copyTo(outputFile, overwrite = true)
                    return@withContext
                }
            } catch (e: Exception) {
                println("Warning: Could not validate PDF header: ${e.message}")
            }
            
            try {
                // Load PDF using PDFBox 3.x API - use() ensures proper closing
                Loader.loadPDF(inputFile).use { document ->
                    // Ensure output directory exists
                    outputFile.parentFile?.mkdirs()
                    
                    // Save with compression enabled - uses Flate compression for content streams
                    // CompressParameters.DEFAULT_COMPRESSION provides good compression while maintaining readability
                    document.save(outputFile, CompressParameters.DEFAULT_COMPRESSION)
                }
                
                // Validate output file was created and has content
                if (!outputFile.exists() || outputFile.length() == 0L) {
                    throw IOException("Compressed file is empty or was not created")
                }
                
                val originalSize = inputFile.length()
                val compressedSize = outputFile.length()
                val reduction = if (originalSize > 0) {
                    ((originalSize - compressedSize) * 100 / originalSize)
                } else {
                    0
                }
                println("PDF compressed: ${formatSize(originalSize)} -> ${formatSize(compressedSize)} ($reduction% reduction)")
            } catch (e: Exception) {
                // Check for specific PDF loading errors
                val errorMessage = e.message ?: ""
                if (errorMessage.contains("Missing root object") || 
                    errorMessage.contains("trailer") ||
                    errorMessage.contains("corrupted") ||
                    errorMessage.contains("invalid")) {
                    println("PDF appears to be corrupted or image-based and cannot be compressed by PDFBox")
                    println("Using original file without compression")
                } else {
                    println("Error compressing PDF: $errorMessage")
                    e.printStackTrace()
                }
                
                // If compression fails, copy the original file
                try {
                    if (inputFile.exists() && inputFile.length() > 0) {
                        // Delete empty output file if it exists
                        if (outputFile.exists() && outputFile.length() == 0L) {
                            outputFile.delete()
                        }
                        inputFile.copyTo(outputFile, overwrite = true)
                        println("Using original file (${formatSize(inputFile.length())}) without compression")
                    } else {
                        println("Error: Cannot fallback - input file is invalid")
                    }
                } catch (copyException: Exception) {
                    println("Error copying original file: ${copyException.message}")
                    copyException.printStackTrace()
                }
            }
        }
    }
    
    fun start() {
        embeddedServer(CIO, port = 8084) {
            install(CallLogging)
            install(ContentNegotiation) {
                jackson()
            }
            install(CORS) {
                allowMethod(io.ktor.http.HttpMethod.Options)
                allowMethod(io.ktor.http.HttpMethod.Post)
                allowMethod(io.ktor.http.HttpMethod.Get)
                allowMethod(io.ktor.http.HttpMethod.Put)
                allowMethod(io.ktor.http.HttpMethod.Delete)
                allowMethod(io.ktor.http.HttpMethod.Patch)
                allowHeader(io.ktor.http.HttpHeaders.ContentType)
                allowHeader(io.ktor.http.HttpHeaders.Authorization)
                anyHost() // Allow requests from any origin
            }

            routing {
                post("/uploadgpt") {
                    floatingWindow.setText("generating")
                    
                    val multipart = call.receiveMultipart(
                        formFieldLimit = 10L * 1024 * 1024 * 1024
                    )

                    val savedFiles = mutableListOf<String>()
                    val compressionResults = mutableListOf<String>()
                    var compressionQuality = 0.5f
                    var questionText = ""

                    val home = System.getProperty("user.home")
                    val uploadsDir = File(home, "uploads")
                    val compressedDir = File(home, "uploads/compressed")
                    if (!uploadsDir.exists()) uploadsDir.mkdirs()
                    if (!compressedDir.exists()) compressedDir.mkdirs()

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val fileName = part.originalFileName ?: "upload.dat"
                                val safeName = fileName.replace(Regex("[^A-Za-z0-9_.-]"), "_")
                                val outFile = File(uploadsDir, "${System.currentTimeMillis()}_$safeName")

                                kotlinx.coroutines.runBlocking {
                                    withContext(Dispatchers.IO) {
                                        val input = part.streamProvider()
                                        val output = outFile.outputStream()

                                        try {
                                            input.copyTo(output, bufferSize = 8192 * 4)
                                        } finally {
                                            runCatching { output.close() }
                                            runCatching { input.close() }
                                        }
                                    }
                                }

                                savedFiles.add(outFile.absolutePath)
                                println("Saved: ${outFile.absolutePath} (${outFile.length()} bytes)")

                                // Compress if it's a PDF
                                if (fileName.endsWith(".pdf", ignoreCase = true)) {
                                    try {
                                        val compressedFile = File(
                                            compressedDir,
                                            "document.pdf"
                                        )

                                        kotlinx.coroutines.runBlocking {
                                            compressPdfFile(outFile, compressedFile, compressionQuality)
                                        }

                                        val originalSize = outFile.length()
                                        val compressedSize = compressedFile.length()
                                        val ratio = if (originalSize > 0) {
                                            ((originalSize - compressedSize) * 100 / originalSize)
                                        } else 0

                                        compressionResults.add(
                                            "✓ $fileName: ${formatSize(originalSize)} → ${formatSize(compressedSize)} ($ratio% reduction)\n" +
                                                    "  Compressed: ${compressedFile.absolutePath}"
                                        )

                                        println("Compressed: ${compressedFile.absolutePath} (${compressedFile.length()} bytes)")
                                    } catch (e: Exception) {
                                        compressionResults.add("✗ $fileName: Compression failed - ${e.message}")
                                        println("Compression failed for $fileName: ${e.message}")
                                        e.printStackTrace()
                                    }
                                }
                            }
                            is PartData.FormItem -> {
                                when (part.name) {
                                    "quality" -> {
                                        compressionQuality = part.value.toFloatOrNull()?.coerceIn(0.1f, 1.0f) ?: 0.5f
                                    }
                                    "question" -> {
                                        questionText = part.value
                                        println("Received question: $questionText")
                                    }
                                }
                            }
                            else -> {}
                        }
                        part.dispose()
                    }

                    if (savedFiles.isEmpty()) {
                        call.respondText(
                            "No files uploaded",
                            status = io.ktor.http.HttpStatusCode.BadRequest
                        )
                    } else {
                        val response = GeminiPdfClient.ask(savedFiles.first())
                        floatingWindow.setText(response)
                        
                        val responseText = buildString {
                            appendLine(response)
                            if (compressionResults.isNotEmpty()) {
                                appendLine("\nCompression Results:")
                                compressionResults.forEach { appendLine(it) }
                            }
                        }

                        call.respondText(responseText, status = io.ktor.http.HttpStatusCode.OK)
                    }
                }
            }
        }.start(wait = true)
    }
}
