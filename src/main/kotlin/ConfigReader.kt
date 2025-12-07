import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

/**
 * Reads configuration from a .properties file located next to the JAR file
 */
object ConfigReader {
    private var properties: Properties? = null
    private var propertiesFile: File? = null

    /**
     * Gets the directory where the JAR file is located
     * Works both when running from JAR and from IDE
     */
    private fun getJarDirectory(): File {
        try {
            // Get the path of the class file
            val codeSource = ConfigReader::class.java.protectionDomain.codeSource
            
            if (codeSource != null) {
                val location = codeSource.location
                if (location != null) {
                    val jarFile = File(location.toURI())
                    if (jarFile.isFile) {
                        // Running from JAR - return the directory containing the JAR
                        return jarFile.parentFile
                    } else {
                        // Running from IDE - return the directory
                        return jarFile
                    }
                }
            }
        } catch (e: Exception) {
            println("Warning: Could not determine JAR location: ${e.message}")
        }
        
        // Fallback to current working directory
        return File(System.getProperty("user.dir"))
    }

    /**
     * Loads the properties file from the JAR directory
     * @param fileName The name of the properties file (default: "config.properties")
     */
    fun loadProperties(fileName: String = "config.properties") {
        try {
            val jarDir = getJarDirectory()
            propertiesFile = File(jarDir, fileName)
            
            if (!propertiesFile!!.exists()) {
                println("Warning: Properties file not found at: ${propertiesFile!!.absolutePath}")
                println("Creating a template properties file...")
                createTemplatePropertiesFile(propertiesFile!!)
                return
            }
            
            properties = Properties().apply {
                FileInputStream(propertiesFile!!).use { input ->
                    load(input)
                }
            }
            
            println("Loaded properties from: ${propertiesFile!!.absolutePath}")
        } catch (e: IOException) {
            println("Error loading properties file: ${e.message}")
            e.printStackTrace()
            properties = null
        }
    }

    /**
     * Creates a template properties file if it doesn't exist
     */
    private fun createTemplatePropertiesFile(file: File) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(
                """
                # Configuration Properties
                # API Key for Gemini
                apikey=YOUR_API_KEY_HERE
                
                """.trimIndent()
            )
            println("Template properties file created at: ${file.absolutePath}")
            println("Please edit the file and add your API key.")
        } catch (e: Exception) {
            println("Error creating template properties file: ${e.message}")
        }
    }

    /**
     * Gets a property value by key
     * @param key The property key
     * @param defaultValue Default value if key is not found
     * @return The property value or defaultValue
     */
    fun getProperty(key: String, defaultValue: String = ""): String {
        if (properties == null) {
            loadProperties()
        }
        return properties?.getProperty(key, defaultValue) ?: defaultValue
    }

    /**
     * Gets the API key from the properties file
     * @return The API key or empty string if not found
     */
    fun getApiKey(): String {
        return getProperty("apikey", "").trim()
    }

    /**
     * Gets the properties file path
     * @return The absolute path to the properties file
     */
    fun getPropertiesFilePath(): String? {
        if (propertiesFile == null) {
            val jarDir = getJarDirectory()
            propertiesFile = File(jarDir, "config.properties")
        }
        return propertiesFile?.absolutePath
    }

    /**
     * Checks if the properties file exists
     */
    fun propertiesFileExists(): Boolean {
        if (propertiesFile == null) {
            val jarDir = getJarDirectory()
            propertiesFile = File(jarDir, "config.properties")
        }
        return propertiesFile?.exists() ?: false
    }
}

