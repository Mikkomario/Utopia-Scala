package utopia.reach.test

import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.StackLayout.{Center, Leading, Trailing}
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.check.{ContextualSwitchFactory, Switch}
import utopia.reach.component.label.text.{TextLabel, ViewTextLabel}
import utopia.reach.container.multi.{SegmentGroup, Stack}
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow

/**
  * A test case for switches.
  *
  * Instructions:
  *     - You should see a window with 4 labels and 2 switches
  *     - The lower switch should control the enabled-state of the upper switch
  *     - You should see a pop-up whenever the enabled-state changes
  *
  * @author Mikko Hilpinen
  * @since 25.11.2020, v0.1
  */
object ReachSwitchTest extends App
{
	import ReachTestContext._
	
	// Controls
	val enabledPointer = new PointerWithEvents(true)
	
	// Creates the components
	// Window
	val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		// Framing
		framingF.build(Stack) { colF =>
			// Y-Stack
			colF.build(Stack) { rowF =>
				// Uses segmentation between the rows
				val rowGroup = SegmentGroup.rowsWithLayouts(Trailing, Center, Leading)
				
				// Creates a single row with 1) name label, 2) switch and 3) switch value label
				def makeRow(fieldName: LocalizedString)(makeSwitch: ContextualSwitchFactory => Switch) = {
					rowF.centered.related.buildSegmented(Mixed, rowGroup) { factories =>
						// 1: Name label
						val nameLabel = factories.next().mapContext {
							_.forTextComponents.withTextAlignment(Alignment.Right)
								.mapFont { _.bold }
						}(TextLabel).apply(fieldName)
						// 2: Switch
						val switch = makeSwitch(factories.next()(Switch))
						// 3: Switch value label
						val valueLabel = factories.next().mapContext { _.forTextComponents }(ViewTextLabel)
							.hint(switch.valuePointer)
						
						// Returns the created switch as an additional result
						Vector(nameLabel, switch, valueLabel) -> switch
					}
				}
				
				// Contains 2 rows:
				// 1) Main switch row and
				val mainRow = makeRow("Switch") { _.withEnabledPointer(enabledPointer).apply() }
				// 2) a switch row which controls enabled state of the first switch
				val enabledRow = makeRow("Enabled") { _.apply(valuePointer = enabledPointer) }
				
				// Returns the switch that controls the enabled state as an additional result
				Vector(mainRow, enabledRow).map { _.parent } -> enabledRow.result
			}
		}
	}
	
	// Displays the window
	window.display(centerOnParent = true)
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	start()
	
	// Shows a pop-up when enabled switch state changes
	window.result.valuePointer.addListener { event =>
		// Pop-up
		val popup = ReachWindow.contentContextual.withWindowBackground(colors.info).borderless.nonFocusable
			.anchoredToUsing(Framing, window.result, Alignment.Right, margins.medium) { (_, framingF) =>
				// Framing
				framingF.small.build(TextLabel) { labelF =>
					// Label (Enabled | Disabled)
					labelF.apply(if (event.newValue) "Enabled!" else "Disabled!")
				}
			}
		popup.setToCloseOnAnyKeyRelease()
		popup.setToCloseAfter(2.seconds)
		popup.display()
		Continue
	}
}
