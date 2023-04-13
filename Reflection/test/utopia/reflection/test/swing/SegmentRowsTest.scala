package utopia.reflection.test.swing

import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.swing.button.TextButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.firmament.model.stack.LengthExtensions._
import utopia.paradigm.color.ColorRole.Primary

/**
  * A test implementation of segmentation using Segment and SegmentGroup classes
  * @author Mikko Hilpinen
  * @since 10.6.2020, v1.2
  */
object SegmentRowsTest extends App
{
	ParadigmDataType.setup()

	import TestContext._

	// Creates the labels
	val backgroundContext = baseContext.against(colorScheme.primary.light)
	val labels = backgroundContext.forTextComponents.withTextAlignment(Alignment.Center).withHorizontallyExpandingText
		.use { implicit c =>
			Vector("Here are some labels", "just for you", "once", "again!").map {
				TextLabel.contextualWithBackground(c.color.secondary, _)
			}
		}

	// Creates buttons as well
	val (button1, button2) = backgroundContext.forTextComponents.withTextAlignment(Alignment.Center).mapFont { _ * 1.2 }
		.withHorizontallyExpandingText.withBackground(Primary)
		.use { implicit c =>
			val button1 = TextButton.contextual("Yeah!") { labels(1).text += "!" }
			val button2 = TextButton.contextual("For Sure!") { labels.last.text += "!" }
			button1 -> button2
		}

	// Creates the rows and the main stack
	val group = new SegmentGroup()
	val stack = backgroundContext.use { implicit c =>
		Stack.buildColumnWithContext() { stack =>
			stack += Stack.buildRowWithContext(isRelated = true) { row1 =>
				group.wrap(labels.take(2) :+ button1).foreach { row1 += _ }
			}
			stack += Stack.buildRowWithContext(isRelated = true) { row2 =>
				group.wrap(labels.drop(2) :+ button2).foreach { row2 += _ }
			}
		}
	}

	val frame = Frame.windowed(stack.framed(margins.medium.downscaling, backgroundContext.background),
		"Segment Test", Program)
	frame.setToCloseOnEsc()

	new SingleFrameSetup(actorHandler, frame).start()
}
