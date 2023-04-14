package utopia.reach.test

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.context.{ColorContext, TextContext}
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.StackLayout.{Center, Leading, Trailing}
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.check.{ContextualSwitchFactory, Switch}
import utopia.reach.component.label.text.{ContextualTextLabelFactory, TextLabel, ViewTextLabel}
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.container.ReachCanvas2
import utopia.reach.container.wrapper.Framing
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic.WhenClickedOutside
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.reach.container.multi.{SegmentGroup, Stack}
import utopia.reflection.util.SingleFrameSetup

/**
  * A test case for switches
  * @author Mikko Hilpinen
  * @since 25.11.2020, v0.1
  */
object ReachSwitchTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	import TestCursors._
	import utopia.reflection.test.TestContext._
	/*
	val (canvas, enabledSwitch) = ReachCanvas2(cursors) { hierarchy =>
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
	
	val frame = Frame.windowed(canvas, "Reach Test", Program, getAnchor = canvas.anchorPosition(_))
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
	
	 */
}
