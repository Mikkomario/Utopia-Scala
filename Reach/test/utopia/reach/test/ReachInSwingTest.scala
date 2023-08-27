package utopia.reach.test

import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.handling.KeyTypedListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorRole.Primary
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.ReachCanvas
import utopia.reach.container.wrapper.AlignFrame

import javax.swing.{JFrame, JPanel, WindowConstants}

/**
  * Tests Reach Canvas -displaying inside a Swing component.
  *
  * What you should see:
  *     - A window with a yellow window, opening at the center of the screen
  *     - A red label somewhere in the upper left area of the window
  *     - A black label at the center of the red label, reading "test"
  *     - Whenever you type a character, that should modify the displayed text
  *
  * @author Mikko Hilpinen
  * @since 27.7.2023, v1.1
  */
object ReachInSwingTest extends App
{
	import ReachTestContext._
	
	private val textPointer = EventfulPointer("test")
	private val window = new TestWindow()
	
	textPointer.addListener { e => println(e.newValue) }
	
	GlobalKeyboardEventHandler += KeyTypedListener { e =>
		val char = e.typedChar.toString
		if (textPointer.value.startsWith(char))
			textPointer.value = char
		else
			textPointer.value += char
	}
	
	window.setVisible(true)
	
	println("Done!")
	
	
	// NESTED   --------------------------
	
	private class TestWindow extends JFrame()
	{
		val panel = new JPanel(null)
		
		panel.setBackground(Color.yellow.toAwt)
		
		setContentPane(panel)
		setSize(300, 200)
		panel.setSize(300, 200)
		setLocationRelativeTo(null)
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
		
		val canvas = ReachCanvas.forSwing(Fixed(Color.transparentBlack), TestCursors.cursors, disableFocus = true) { hierarchy =>
			AlignFrame(hierarchy).withContext(baseContext.against(Color.red).forTextComponents)(Center)
				.build(ViewTextLabel) { labelF =>
					labelF.withBackground(Primary)(textPointer)
				}
		}.parent
		canvas.position = Point(20, 20)
		canvas.size = Size(200, 50)
		canvas.updateLayout()
		
		canvas.attachmentPointer.addListenerAndSimulateEvent(false) { e =>
			if (e.newValue)
				println("Canvas is now attached")
			else
				println("Canvas detached")
		}
		
		println(panel.getWidth)
		
		panel.add(canvas.component)
	}
}
