package utopia.reflection.test.swing

import java.awt.event.KeyEvent

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.handling.KeyStateListener
import utopia.paradigm.shape.shape2d.Size
import utopia.reflection.component.swing.label.{EmptyLabel, TextLabel}
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.layout.wrapper.AnimatedSwitchPanel
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.util.SingleFrameSetup

/**
  * Tests animated switch panel
  * @author Mikko Hilpinen
  * @since 30.8.2020, v1.2
  */
object AnimatedSwitchPanelTest extends App
{
	import utopia.reflection.test.TestContext._
	
	val label1 = baseContext.inContextWithBackground(colorScheme.primary).forTextComponents.mapFont { _ * 2 }
		.use { implicit c => TextLabel.contextual("This is a test") }
	label1.background = colorScheme.primary
	val label2 = new EmptyLabel().withStackSize(StackSize.any(Size(320, 128)))
	label2.background = colorScheme.secondary
	val label3 = new EmptyLabel().withStackSize(StackSize.any(Size(96, 96)))
	label3.background = colorScheme.primary.light
	val label4 = new EmptyLabel().withStackSize(StackSize.any(Size(228, 64)))
	label4.background = colorScheme.primary.dark

	val labels = Vector(label1, label2, label3, label4)

	val panel = AnimatedSwitchPanel.contextual[AwtStackable](label1)
	val frame = Frame.windowed(panel, "AnimatedSwitchPanel Test", Program)

	val setup = new SingleFrameSetup(actorHandler, frame)
	setup.start()

	val indexPointer = new PointerWithEvents(0)
	frame.addKeyStateListener(KeyStateListener.onAnyKeyPressed { event =>
		if (event.index == KeyEvent.VK_RIGHT)
			indexPointer.update { i => (i + 1) % labels.size }
		else if (event.index == KeyEvent.VK_LEFT)
			indexPointer.update { i =>
				val newI = i - 1
				if (newI < 0)
					newI + labels.size
				else
					newI
			}
	})
	indexPointer.addContinuousListener { e => panel.set(labels(e.newValue)) }
}
