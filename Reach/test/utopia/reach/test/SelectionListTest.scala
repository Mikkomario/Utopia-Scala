package utopia.reach.test

import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.handling.KeyTypedListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.generic.ParadigmDataType
import utopia.reach.component.button.text.TextButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.selection.{ContextualSelectionListFactory, SelectionList}
import utopia.reach.component.label.text.MutableViewTextLabel
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.stack.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reflection.component.context.TextContext
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.util.SingleFrameSetup

/**
  * Tests selection list class
  * @author Mikko Hilpinen
  * @since 6.2.2021, v0.1
  */
object SelectionListTest extends App
{
	ParadigmDataType.setup()
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	import TestCursors._
	import utopia.reflection.test.TestContext._
	
	val contentPointer = new PointerWithEvents(Vector(1, 2, 3, 4, 5))
	val valuePointer = new PointerWithEvents[Option[Int]](Some(2))
	
	val mainBg = colorScheme.gray.default
	
	val canvas = ReachCanvas(cursors) { hierarchy =>
		Stack(hierarchy).withContext(baseContext).build(Mixed)
			.column(customDrawers = Vector(BackgroundDrawer(mainBg))) { factories =>
				val framingBg = colorScheme.primary.light
				val framing = baseContext.inContextWithBackground(framingBg).forTextComponents.expandingToRight.use { implicit c =>
					factories(Framing).withContext(c).build(SelectionList)
						.apply(margins.small.any, customDrawers = Vector(BackgroundDrawer(framingBg))) { listF: ContextualSelectionListFactory[TextContext] =>
							listF.apply(contentPointer, valuePointer, alternativeKeyCondition = true) { (hierarchy, item: Int) =>
								val label = MutableViewTextLabel(hierarchy).withContext(c)
									.apply(item, DisplayFunction.interpolating("Label %s"))
								// label.addBackground(Color.cyan)
								label
							}
						}
				}
				val button = factories.withContext(
					baseContext.inContextWithBackground(mainBg).forTextComponents.forPrimaryColorButtons)(TextButton)
					.apply("Button") { println("Button Pressed") }
				
				Vector(framing.parent, button)
			}.parent
	}
	
	GlobalKeyboardEventHandler += KeyTypedListener { event =>
		event.digit.foreach { i => contentPointer.value = (1 to i).toVector }
	}
	
	contentPointer.addListener { e => println(s"Content: $e") }
	valuePointer.addListener { e => println(s"Main value: $e") }
	
	val frame = Frame.windowed(canvas.parent, "Reach Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
