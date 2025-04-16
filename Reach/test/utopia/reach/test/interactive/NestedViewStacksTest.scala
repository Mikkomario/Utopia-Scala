package utopia.reach.test.interactive

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Identity
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.genesis.handling.event.keyboard.Key.FunctionKey
import utopia.genesis.handling.event.keyboard.KeyStateListener
import utopia.reach.component.factory.Mixed
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.container.multi.ViewStack
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
  * Tests view stack hierarchy management when there are nested stacks,
  * with events during the hidden period.
  * @author Mikko Hilpinen
  * @since 15.04.2025, v1.6
  */
object NestedViewStacksTest extends App
{
	// ATTRIBUTES   -----------------------
	
	private val vp1 = ResettableFlag()
	private val vp2 = ResettableFlag()
	
	private val window = ReachWindow.contentContextual.using(ViewStack) { (_, stackF) =>
		stackF.build(Mixed) { factories =>
			val constantLabel = factories.next()(TextLabel).apply("Press 1 to show component")
			val nestedFactory = factories.next()
			val nestedStack = nestedFactory(ViewStack).build(TextLabel) { labelFactories =>
				val constantLabel2 = labelFactories.next()("Press 2 to show another component")
				val toggleLabel = labelFactories.next()("Press 1 and/or 2 to toggle visibility")
				
				val toggleVisibleFlag = vp2.mapWhile(nestedFactory.hierarchy.linkedFlag)(Identity)
				println("Setting up toggle visibility flag")
				toggleVisibleFlag.addListener { e => println(s"Label visibility $e") }
				nestedFactory.hierarchy.linkedFlag.addListener { e => println(s"Stack visibility $e") }
				Pair(constantLabel2 -> AlwaysTrue, toggleLabel -> toggleVisibleFlag)
			}
			
			ComponentCreationResult(Pair(constantLabel -> AlwaysTrue, nestedStack.parent -> vp1), nestedStack.parent)
		}
	}
	private val nestedStack = window.result
	
	
	// APP CODE ---------------------------
	
	window.keyStateHandler += KeyStateListener.pressed.digitRange(1, 2) { e =>
		e.digit.foreach {
			case 1 => vp1.switch()
			case _ => vp2.switch()
		}
	}
	window.keyStateHandler += KeyStateListener.pressed(FunctionKey(5)) { _ =>
		println(nestedStack.components.size)
		println("Revalidating stack")
		nestedStack.revalidate()
	}
	
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	
	start()
	window.display(centerOnParent = true)
}
