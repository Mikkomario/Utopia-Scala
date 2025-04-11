package utopia.reach.test.interactive

import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.genesis.handling.event.keyboard.KeyStateListener
import utopia.paradigm.color.ColorRole.Secondary
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.container.multi.ViewStack
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
  * Displays a view stack with 2 elements, one of which is static.
  * 1 key toggles the display of the second label.
  * @author Mikko Hilpinen
  * @since 11.04.2025, v1.6
  */
object AppearingViewTest extends App
{
	private val labelSize = 320.any x 32.any
	private val displayFlag = ResettableFlag()
	
	private val window = ReachWindow.contentContextual.using(ViewStack) { (_, stackF) =>
		stackF.withoutMargin.build(EmptyLabel) { factories =>
			val l1 = factories.next()(labelSize)
			val l2 = factories.next().withBackground(Secondary)(labelSize)
			
			Pair(l1 -> AlwaysTrue, l2 -> displayFlag)
		}
	}
	
	window.keyStateHandler += KeyStateListener.pressed.digit(1) { _ => displayFlag.switch() }
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	
	window.display(centerOnParent = true)
}
