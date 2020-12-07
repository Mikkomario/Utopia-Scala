package utopia.reflection.test.reach

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.Changing
import utopia.reflection.component.context.ColorContext
import utopia.reflection.component.reach.factory.Mixed
import utopia.reflection.component.reach.input.{ContextualSwitchFactory, Switch}
import utopia.reflection.component.reach.label.{TextLabel, ViewTextLabel}
import utopia.reflection.component.reach.wrapper.ComponentCreationResult
import utopia.reflection.container.reach.{Framing, SegmentGroup, Stack}
import utopia.reflection.container.stack.StackLayout.{Center, Leading, Trailing}
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic.WhenClickedOutside
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.util.{AwtEventThread, SingleFrameSetup}

/**
  * A test case for switches
  * @author Mikko Hilpinen
  * @since 25.11.2020, v2
  */
object ReachSwitchTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	import utopia.reflection.test.TestContext._
	
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
						val nameLabel = factories.next().mapContext { _.forTextComponents.withTextAlignment(Alignment.Right)
							.mapFont { _.bold } }(TextLabel).apply(fieldName)
						val switch = makeSwitch(factories.next()(Switch))
						ComponentCreationResult(Vector(nameLabel, switch,
							// Switch value label
							factories.next().mapContext { _.forTextComponents }(ViewTextLabel)
								.apply(switch.valuePointer, isHintPointer = Changing.wrap(true))
						), switch)
					}
				}
				// Contains 2 rows:
				// 1) Main switch row and
				// 2) a switch row which controls enabled state of the first switch
				val enabledPointer = new PointerWithEvents(true)
				val enabledRow = makeRow("Enabled") { _(enabledPointer) }
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
				Framing(hierarchy).buildFilledWithMappedContext(baseContext, colorScheme.info,
					TextLabel) { _.forTextComponents }.rounded(margins.small.any) {
						_.apply(if (event.newValue) "Enabled!" else "Disabled!")
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
