package utopia.reach.test

import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.event.MouseButtonStateEvent
import utopia.genesis.handling.{KeyTypedListener, MouseButtonStateListener}
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorRole.{Primary, Secondary}
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
  *     - A black label at the center of an orange label, reading "test"
  *     - Whenever you type a character, that should modify the displayed text
  *     - Whenver you click the label, that should change window color
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
	
	start()
	window.setVisible(true)
	
	println("Done!")
	
	
	// NESTED   --------------------------
	
	private class TestWindow extends JFrame()
	{
		private val clicksCounter = EventfulPointer(0)
		
		private val panel = new JPanel(null)
		
		clicksCounter.addContinuousListenerAndSimulateEvent(-1) { clicks =>
			val color = if (clicks.newValue % 2 == 0) Color.yellow else Color.cyan
			panel.setBackground(color.toAwt)
		}
		
		setContentPane(panel)
		setSize(300, 200)
		panel.setSize(300, 200)
		setLocationRelativeTo(null)
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
		
		private val canvas = ReachCanvas.forSwing(baseContext.actorHandler, Fixed(Color.transparentBlack), TestCursors.cursors,
			disableFocus = true) { hierarchy =>
			AlignFrame(hierarchy).withContext(baseContext.against(Color.yellow).forTextComponents)(Center)
				.withBackground(Secondary)
				.build(ViewTextLabel) { labelF =>
					val label = labelF.withBackground(Primary)(textPointer)
					label.addMouseButtonListener(MouseButtonStateListener(MouseButtonStateEvent.leftPressedFilter) { event =>
						if (label.bounds.contains(event.mousePosition))
							clicksCounter.update { _ + 1 }
						else
							println("Click outside")
						println(s"\tMouse: ${event.mousePosition}")
						println(s"\tlabel: ${label.bounds}")
						None
					})
					label
				}
		}
		canvas.position = Point(20, 100)
		canvas.size = Size(200, 50)
		canvas.updateLayout()
		canvas.child.addMouseButtonListener(MouseButtonStateListener(MouseButtonStateEvent.leftPressedFilter) { event =>
			println(s"Align frame Mouse: ${event.mousePosition}")
			None
		})
		
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
