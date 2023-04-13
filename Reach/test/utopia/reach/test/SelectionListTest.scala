package utopia.reach.test

import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.handling.KeyTypedListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.color.ColorRole.Primary
import utopia.paradigm.generic.ParadigmDataType
import utopia.reach.component.button.text.TextButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.selection.SelectionList
import utopia.reach.component.label.text.MutableViewTextLabel
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.stack.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.container.wrapper.scrolling.ScrollView
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
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
		Framing(hierarchy).withContext(baseContext).build(Stack)
			.apply(margins.medium.any, customDrawers = Vector(BackgroundDrawer(mainBg))) { stackF =>
				stackF.build(Mixed).column() { factories =>
					val scroll = factories(ScrollView).build(Framing)(maxOptimalLength = Some(224)) { framingF =>
						val framingBg = colorScheme.primary.light
						baseContext.against(framingBg).forTextComponents.withTextExpandingToRight.use { implicit c =>
							framingF.withContext(c).build(SelectionList)
								.apply(margins.small.any, customDrawers = Vector(BackgroundDrawer(framingBg))) { listF =>
									listF.apply(contentPointer, valuePointer, alternativeKeyCondition = true) { (hierarchy, item: Int) =>
										val label = MutableViewTextLabel(hierarchy).withContext(c)
											.apply(item, DisplayFunction.interpolating("Label %s"))
										// label.addBackground(Color.cyan)
										label
									}
								}
						}
					}
					val button = factories.withContext(baseContext.against(mainBg).forTextComponents/Primary)(TextButton)
						.apply("Button") { println("Button Pressed") }
					
					Vector(scroll.parent, button)
				}
			}
			.parent
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
