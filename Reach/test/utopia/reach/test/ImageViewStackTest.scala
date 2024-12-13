package utopia.reach.test

import utopia.firmament.model.enumeration.SizeCategory.Medium
import utopia.firmament.model.stack.StackSize
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.handling.event.keyboard.{KeyStateListener, KeyboardEvents}
import utopia.genesis.image.Image
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.factory.Mixed
import utopia.reach.component.label.image.{ImageLabel, ViewImageLabel}
import utopia.reach.container.multi.ViewStack
import utopia.reach.window.ReachWindow

import java.awt.event.KeyEvent

/**
  * Tests combination of image labels and view stack.
  *
  * Instructions:
  *     - Keys 1 and 2 control label visibility
  *     - Key 3 switches the label 2 between 2 different sized images
  *     - Stack should be centered vertically
  *     - Window size should remain fixed
  *
  * @author Mikko Hilpinen
  * @since 26.1.2021, v0.1
  */
object ImageViewStackTest extends App
{
	import ReachTestContext._
	
	val icon1 = Image.readFrom("Reach/test-images/cursor-arrow.png").get
	val icon2 = Image.readFrom("Reach/test-images/cursor-hand.png").get.withCenterOrigin * 2
	val icon3 = Image.readFrom("Reach/test-images/cursor-text.png").get
	
	val pointer1 = EventfulPointer(true)
	val pointer2 = EventfulPointer(false)
	val pointer3 = EventfulPointer(false)
	
	// Window[Stack[Label1, Label2]]
	val window = ReachWindow.contentContextual.using(ViewStack) { (_, stackF) =>
		stackF.row.centered.build(Mixed) { factories =>
			Vector(
				// The first label is static, but sometimes disappears
				factories.next()(ImageLabel).withBackground(Color.green).withColor(Secondary).withInsets(Medium)
					.apply(icon1) -> pointer1,
				// The second label switches between two icons, disappearing based on pointer value
				factories.next().variable(ViewImageLabel).withBackground(Color.cyan)
					.apply(pointer3.map { if (_) icon2 else icon3 }) -> pointer2
			)
		}
	}
	
	// Adjusts the label visibilities with keys 1, 2 and 3
	KeyboardEvents += KeyStateListener.pressed { event =>
		event.index match {
			case KeyEvent.VK_1 => pointer1.update { !_ }
			case KeyEvent.VK_2 => pointer2.update { !_ }
			case KeyEvent.VK_3 => pointer3.update { !_ }
		}
	}
	
	// Sets a fixed size for the content
	val testSize = Size(128, 64)
	window.content.addConstraint { _ => StackSize.fixed(testSize) }
	
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	start()
}
