import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO


fun main(array: Array<String>) {
    Solution.main(array);
}


class GlobalKeyListenerExample : NativeKeyListener {
    private var ctrlPressed = false
    private var commandPressed = false
    private var mPressed = false
    val floatingWindow: FloatingWindow = FloatingWindow()

    override fun nativeKeyPressed(e: NativeKeyEvent) {
        when (e.keyCode) {
            29 -> {
                ctrlPressed = true
            }

            3675 ->
                commandPressed = true

            50 ->
                mPressed = true;
            else -> {
                ctrlPressed = false
                commandPressed = false
                mPressed = false
            }
        }
        if (ctrlPressed && mPressed && commandPressed) {
            floatingWindow.setText("loading")
            takeScreenshot()
//            var response = GeminiClient.Caller.sendImageToGemini(imagePath = "${Constants.UserHome}/screenshot.png")
//            floatingWindow.setText(response)
        }
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) {
        if (e.getKeyCode() == NativeKeyEvent.VC_CONTROL) {
            ctrlPressed = false
        }
    }

    override fun nativeKeyTyped(e: NativeKeyEvent?) {
        // Not needed for Ctrl/C detection
    }

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                GlobalScreen.registerNativeHook()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val exmaple = GlobalKeyListenerExample()

            GlobalScreen.addNativeKeyListener(exmaple)
            GlobalScope.launch {
                RestServer(exmaple.floatingWindow).start()
            }
            println("server started")
        }
    }

    fun takeScreenshot(filename: String = "${Constants.UserHome}/screenshot.png") {
        // Get screen size
        val screenSize = Toolkit.getDefaultToolkit().screenSize

        // Create a robot instance to capture the screen
        val robot = Robot()

        // Capture the whole screen
        val screenRect = Rectangle(screenSize)
        val image: BufferedImage = robot.createScreenCapture(screenRect)

        // Save the image
        ImageIO.write(image, "png", File(filename))

        println("Screenshot saved to $filename")
    }
}