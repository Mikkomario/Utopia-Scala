package utopia.reach.test

import utopia.firmament.localization.DisplayFunction
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.handling.KeyTypedListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.color.ColorRole.Primary
import utopia.paradigm.color.ColorShade.Light
import utopia.reach.component.button.text.TextButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.selection.SelectionList
import utopia.reach.component.label.text.MutableViewTextLabel
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.container.wrapper.scrolling.ScrollView
import utopia.reach.window.ReachWindow

/**
  * Tests selection list class
  * @author Mikko Hilpinen
  * @since 6.2.2021, v0.1
  */
object SelectionListTest extends App
{
	import ReachTestContext._
	
	// Data
	val contentPointer = new PointerWithEvents(Vector(1, 2, 3, 4, 5))
	val valuePointer = new PointerWithEvents[Option[Int]](Some(2))
	
	val mainBg = colors.gray.default
	
	// Creates the components
	val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		// Framing
		framingF.build(Stack).apply(margins.aroundMedium) { stackF =>
			// Stack[ScrollView + Button]
			stackF.build(Mixed) { factories =>
				// 1: Scroll View
				val scroll = factories(ScrollView).build(Framing)(maxOptimalLength = Some(224)) { framingF =>
					// Framing
					framingF.buildFilled(Primary, SelectionList, Light).apply(margins.aroundSmall) { listF =>
						// Selection list
						listF.apply(contentPointer, valuePointer, alternativeKeyCondition = true) { (hierarchy, item: Int) =>
							MutableViewTextLabel(hierarchy).withContext(listF.context)
								.mapTextInsets { _.mapRight { _ + margins.large } }
								.apply(item, DisplayFunction.interpolating("Label %s"))
						}
					}
				}
				// 2: Button
				val button = factories.withContext(baseContext.against(mainBg).forTextComponents / Primary)(TextButton)
					.apply("Button") { println("Button Pressed") }
				
				Vector(scroll.parent, button)
			}
		}
	}
	
	// Changes content based on digit key-presses
	GlobalKeyboardEventHandler += KeyTypedListener { event =>
		event.digit.foreach { i => contentPointer.value = (1 to i).toVector }
	}
	
	contentPointer.addListener { e => println(s"Content: $e") }
	valuePointer.addListener { e => println(s"Main value: $e") }
	
	// Displays the window
	window.display(centerOnParent = true)
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	start()
}
