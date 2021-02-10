package utopia.reach.test

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.AlwaysTrue
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.{ContextualSwitchFactory, Switch}
import utopia.reach.component.label.{ContextualTextLabelFactory, TextLabel, ViewTextLabel}
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.container.{Framing, ReachCanvas, SegmentGroup, Stack}
import utopia.reflection.component.context.{ColorContext, TextContext}
import utopia.reflection.container.stack.StackLayout.{Center, Leading, Trailing}
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic.WhenClickedOutside
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.util.{AwtEventThread, SingleFrameSetup}
import utopia.reflection.shape.LengthExtensions._

/**
  * A test case for switches
  * @author Mikko Hilpinen
  * @since 25.11.2020, v2
  */
object ReachSwitchTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	val (canvas, enabledSwitch) = ReachCanvas(cursors) { hierarchy =>
		/*Framing(hierarchy).withContext(baseContext).buildFilled(colorScheme.gray, Stack).apply(margins.medium.any) { colF =>
		
		}*/
		Framing(hierarchy).buildFilledWithContext(baseContext, colorScheme.gray, Stack).apply(margins.medium.any) { colF =>
			colF.build(Stack).column() { rowF =>
				val rowGroup = SegmentGroup.rowsWithLayouts(Trailing, Center, Leading)
				
				// Creates a single row with 1) name label, 2) switch and 3) switch value label
				def makeRow(fieldName: LocalizedString)(makeSwitch: ContextualSwitchFactory[ColorContext] => Switch) =
				{
					rowF.build(Mixed).segmented(rowGroup, layout = Center, areRelated = true) { factories =>
						val nameLabel = factories.next().mapContext {
							_.forTextComponents.withTextAlignment(Alignment.Right)
								.mapFont { _.bold }
						}(TextLabel).apply(fieldName)
						val switch = makeSwitch(factories.next()(Switch))
						ComponentCreationResult(Vector(nameLabel, switch,
							// Switch value label
							factories.next().mapContext { _.forTextComponents }(ViewTextLabel)
								.apply(switch.valuePointer, isHintPointer = AlwaysTrue)
						), switch)
					}
				}
				
				// Contains 2 rows:
				// 1) Main switch row and
				// 2) a switch row which controls enabled state of the first switch
				val enabledPointer = new PointerWithEvents(true)
				val enabledRow = makeRow("Enabled") { _ (enabledPointer) }
				Vector(
					makeRow("Switch") { _.apply(enabledPointer = enabledPointer) },
					enabledRow
				).map { _.parent } -> enabledRow.result
			}
		}
	}.parentAndResult
	
	// Shows a pop-up when enabled switch state changes
	enabledSwitch.valuePointer.addListener { event =>
		AwtEventThread.async {
			val popup = enabledSwitch.createPopup(actorHandler, margin = margins.medium, autoCloseLogic = WhenClickedOutside) { hierarchy =>
				Framing(hierarchy).buildFilledWithMappedContext[ColorContext, TextContext, ContextualTextLabelFactory](
					baseContext, colorScheme.info, TextLabel) { _.forTextComponents }
					.rounded(margins.small.any) {
						_.apply(if (event.newValue) "Enabled!"
						else "Disabled!")
					}
				// TextLabel(hierarchy).withContext(baseContext.inContextWithBackground(colorScheme.info).forTextComponents)
			}
			popup.display(gainFocus = false)
		}
	}
	
	val frame = Frame.windowed(canvas, "Reach Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
