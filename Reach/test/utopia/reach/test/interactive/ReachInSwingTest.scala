package utopia.reach.test.interactive

import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.{EventfulPointer, AssignableOnce}
import utopia.genesis.handling.event.keyboard.{KeyTypedListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.MouseButtonStateListener
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorRole.{Primary, Secondary}
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.container.ReachCanvas
import utopia.reach.container.wrapper.AlignFrame
import utopia.reach.drawing.MousePositionDrawer
import utopia.reach.test.{ReachTestContext, TestCursors}

import javax.swing.{JFrame, JPanel, WindowConstants}

/**
  * Tests Reach Canvas -displaying inside a Swing component.
  *
  * What you should see:
  *     - A window with a yellow window, opening at the center of the screen
  *     - A black label at the center of an orange label, reading "test"
  *     - Whenever you type a character, that should modify the displayed text
  *     - Whenever you click the label, that should change window color
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
	
	KeyboardEvents += KeyTypedListener.unconditional { e =>
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
		
		private val outerPanel = new JPanel(null)
		private val innerPanel = new JPanel(null)
		outerPanel.add(innerPanel)
		
		outerPanel.setBackground(Color.magenta.toAwt)
		clicksCounter.addContinuousListenerAndSimulateEvent(-1) { clicks =>
			val color = if (clicks.newValue % 2 == 0) Color.yellow else Color.cyan
			innerPanel.setBackground(color.toAwt)
		}
		
		setContentPane(outerPanel)
		setSize(300, 300)
		outerPanel.setSize(300, 300)
		innerPanel.setLocation(30, 30)
		innerPanel.setSize(240, 240)
		setLocationRelativeTo(null)
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
		
		private val canvas = ReachCanvas.forSwing(baseContext.actorHandler, Fixed(Color.transparentBlack),
			TestCursors.cursors, disableFocus = true) { hierarchy =>
			AlignFrame(hierarchy).withContext(baseContext.against(Color.yellow).forTextComponents)(Center)
				.withBackground(Secondary)
				.build(ViewTextLabel) { labelF =>
					val componentP = AssignableOnce[ReachComponentLike]()
					val label = labelF.withBackground(Primary)
						.withCustomDrawer(new MousePositionDrawer(componentP, 3.0))
						.apply(textPointer)
					componentP.set(label)
					label.addMouseButtonListener(MouseButtonStateListener.leftPressed { event =>
						if (label.bounds.contains(event.position))
							clicksCounter.update { _ + 1 }
						else
							println("Click outside")
						println(s"\tMouse: ${event.position.relative}")
						println(s"\tlabel: ${label.bounds}")
					})
					label
				}
		}
		canvas.position = Point(20, 100)
		canvas.size = Size(200, 50)
		canvas.updateLayout()
		canvas.child.addMouseButtonListener(MouseButtonStateListener.leftPressed { event =>
			println(s"Align frame Mouse: ${event.position.relative}")
		})
		
		canvas.attachmentPointer.addListenerAndSimulateEvent(false) { e =>
			if (e.newValue)
				println("Canvas is now attached")
			else
				println("Canvas detached")
		}
		
		println(innerPanel.getWidth)
		
		innerPanel.add(canvas.component)
	}
}
