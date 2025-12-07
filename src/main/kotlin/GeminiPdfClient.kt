import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentResponse
import java.io.File
import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.FileImageOutputStream


/**
 * Client for submitting PDFs to Google Gemini AI
 *
 * Usage:
 * val client = GeminiPdfClient("YOUR_API_KEY")
 * val response = client.analyzePdf(File("document.pdf"), "Summarize this document")
 * println(response)
 */
object GeminiPdfClient {
    // Load API key from properties file, fallback to hardcoded if not found
    private val apiKey: String by lazy {
        val keyFromFile = ConfigReader.getApiKey()
        if (keyFromFile.isNotEmpty() && keyFromFile != "YOUR_API_KEY_HERE") {
            keyFromFile
        } else {
            println("Warning: API key not found in properties file, using default")
            "AIzaSyCQeKkS0ehucaZKBtOVvqRyhkqPfxWkBZA"
        }
    }
    
    init {
        // Load properties file on initialization
        ConfigReader.loadProperties()
    }
    
    /**
     * Compresses a PNG image to reduce file size while maintaining readability
     * Resizes large images and applies PNG compression
     * @param inputFile The input PNG file
     * @param outputFile The output compressed PNG file
     * @param maxWidth Maximum width (default: 1920px for good readability)
     * @param maxHeight Maximum height (default: 1920px for good readability)
     */
    private fun compressPng(inputFile: File, outputFile: File, maxWidth: Int = 1920, maxHeight: Int = 1920): File {
        try {
            // Read the original image
            val originalImage = ImageIO.read(inputFile)
            if (originalImage == null) {
                println("Warning: Could not read image, using original file")
                return inputFile
            }
            
            val originalWidth = originalImage.width
            val originalHeight = originalImage.height
            
            // Calculate new dimensions maintaining aspect ratio
            var newWidth = originalWidth
            var newHeight = originalHeight
            var needsResize = false
            
            if (originalWidth > maxWidth || originalHeight > maxHeight) {
                val widthRatio = maxWidth.toDouble() / originalWidth
                val heightRatio = maxHeight.toDouble() / originalHeight
                val ratio = minOf(widthRatio, heightRatio)
                
                newWidth = (originalWidth * ratio).toInt()
                newHeight = (originalHeight * ratio).toInt()
                needsResize = true
                
                println("Resizing PNG: ${originalWidth}x${originalHeight} -> ${newWidth}x${newHeight}")
            }
            
            // Ensure output directory exists
            outputFile.parentFile?.mkdirs()
            
            val imageToSave = if (needsResize) {
                // Create scaled image with better quality rendering
                val scaledImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
                val graphics = scaledImage.createGraphics()
                // Use high-quality rendering hints
                graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY)
                graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
                graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null)
                graphics.dispose()
                scaledImage
            } else {
                // Use original image if no resize needed
                originalImage
            }
            
            // Write PNG with compression
            val writers = ImageIO.getImageWritersByFormatName("png")
            if (writers.hasNext()) {
                val writer = writers.next()
                val writeParam = writer.defaultWriteParam
                
                // PNG compression is lossless, but we can set compression mode
                if (writeParam.canWriteCompressed()) {
                    writeParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    // For PNG, compression type is more important than quality
                    // Use maximum compression (9) for smaller file size
                    writeParam.compressionType = "Deflate"
                }
                
                val output = FileImageOutputStream(outputFile)
                writer.output = output
                writer.write(null, javax.imageio.IIOImage(imageToSave, null, null), writeParam)
                writer.dispose()
                output.close()
                
                val originalSize = inputFile.length()
                val compressedSize = outputFile.length()
                val reduction = if (originalSize > 0) {
                    ((originalSize - compressedSize) * 100 / originalSize)
                } else 0
                
                println("PNG compressed: ${originalSize / 1024}KB -> ${compressedSize / 1024}KB ($reduction% reduction)")
                return outputFile
            } else {
                // Fallback: use ImageIO.write (basic compression)
                ImageIO.write(imageToSave, "png", outputFile)
                val originalSize = inputFile.length()
                val compressedSize = outputFile.length()
                println("PNG saved: ${originalSize / 1024}KB -> ${compressedSize / 1024}KB")
                return outputFile
            }
        } catch (e: Exception) {
            println("Error compressing PNG: ${e.message}")
            e.printStackTrace()
            // Return original file if compression fails
            return inputFile
        }
    }
    
    fun ask(path: String): String {
        val client = Client.Builder().apiKey(apiKey).build()
        val originalFile = File(path)
        
        if (!originalFile.exists()) {
            throw IllegalArgumentException("File not found: $path")
        }
        
        // Compress PNG if it's a PNG file
        val fileToUse = if (path.lowercase().endsWith(".png")) {
            val compressedFile = File(originalFile.parent, "${originalFile.nameWithoutExtension}_compressed.png")
            compressPng(originalFile, compressedFile)
        } else {
            originalFile
        }
        
        // Determine MIME type
        val mimeType = when {
            path.lowercase().endsWith(".png") -> "image/png"
            path.lowercase().endsWith(".jpg") || path.lowercase().endsWith(".jpeg") -> "image/jpeg"
            path.lowercase().endsWith(".pdf") -> "application/pdf"
            else -> "image/png" // default
        }

        val content: Content? =
            Content.fromParts(
                com.google.genai.types.Part.fromText("please answer this"),
                com.google.genai.types.Part.fromBytes(fileToUse.readBytes(), mimeType)
            )

        val response: GenerateContentResponse =
            client.models.generateContent(
                "gemini-2.5-flash"
                //   "gemini-3-pro-preview"

                , content, null)

        val result = response.text();
        println(result)
        return result.orEmpty();
    }
}