import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class FloatingWindow {
    val frame: Frame = Frame("GPT Output")
    val textArea = TextArea()

    init {
        frame.setAlwaysOnTop(true)
        frame.setSize(400, 300)
        frame.setLayout(BorderLayout())

        // TextArea inside a scroll pane
        textArea.isEditable = false
        frame.add(textArea, BorderLayout.CENTER)

        // Close button
        val close = Button("Close")
        close.addActionListener { frame.dispose() }
        frame.add(close, BorderLayout.SOUTH)

        // Handle window close event to exit the application
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                frame.dispose()
                System.exit(0) // Ensure full app exit
            }
        })

        // Center window and make visible
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }

    fun setText(text: String) {
        textArea.text = text
    }
}