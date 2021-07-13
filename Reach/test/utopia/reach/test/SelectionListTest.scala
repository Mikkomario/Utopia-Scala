package utopia.reach.test

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.KeyTypedListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.reach.component.input.selection.SelectionList
import utopia.reach.component.label.text.MutableViewTextLabel
import utopia.reach.container.ReachCanvas
import utopia.reflection.component.context.TextContext
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.util.SingleFrameSetup

/**
  * Tests selection list class
  * @author Mikko Hilpinen
  * @since 6.2.2021, v0.1
  */
object SelectionListTest extends App
{
	GenesisDataType.setup()
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	val contentPointer = new PointerWithEvents(Vector(1, 2, 3, 4, 5))
	val valuePointer = new PointerWithEvents[Option[Int]](None)
	
	val canvas = ReachCanvas(cursors) { hierarchy =>
		val background = colorScheme.primary.light
		implicit val context: TextContext = baseContext.inContextWithBackground(background)
			.forTextComponents.expandingToRight
		val list = SelectionList(hierarchy).contextual.apply(contentPointer, valuePointer) { (hierarchy, item: Int) =>
			MutableViewTextLabel(hierarchy).contextual.apply(item, DisplayFunction.interpolating("Label %s"))
		}
		list.addCustomDrawer(BackgroundDrawer(background))
		list
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
