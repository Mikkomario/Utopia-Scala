package utopia.reach.test

import utopia.firmament.component.Window
import utopia.firmament.localization.LocalString._
import utopia.firmament.model.HotKey
import utopia.flow.view.mutable.eventful.{PointerWithEvents, SettableOnce}
import utopia.genesis.event.{KeyStateEvent, KeyTypedEvent}
import utopia.genesis.handling.{KeyStateListener, KeyTypedListener}
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.color.ColorRole.{Primary, Secondary}
import utopia.paradigm.color.ColorShade.Light
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Axis.X
import utopia.reach.component.button.text.TextButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.text.EditableTextLabel
import utopia.reach.component.label.text.{MutableTextLabel, TextLabel}
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.focus.FocusListener
import utopia.reach.window.ReachWindow

import java.awt.event.KeyEvent

/**
  * A simple test for the reach component implementation
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
object ReachComponentTest extends App
{
	import ReachTestContext._
	
	private def focusReporter(componentName: String) =
		FocusListener { event => println(s"$componentName: $event") }
	
	// Creates the components
	private val windowPointer = SettableOnce[Window]()
	val window = ReachWindow.contentContextual.using(Stack) { (_, stackF) =>
		// Column
		stackF.withoutMargin.build(Mixed) { factories =>
			// 1: Framing (Secondary)
			val (framing, label) = factories(Framing).withBackground(Secondary, Light)
				.build(MutableTextLabel) { labelF =>
					// Hello Label (Primary)
					labelF.withHorizontallyCenteredText.withBackground("Hello!", Primary)
				}.toTuple
			// 2: Hello Label 2
			val label2 = factories(TextLabel).withTextAlignment(Alignment.Center)
				.apply("Hello 2\nThis label contains 2 lines")
			// 3: Framing
			val editLabelFraming = factories(Framing).withBackground(Primary, Light).build(Stack) { stackF =>
					// Column (Centered)
					stackF.centered.build(Mixed) { factories =>
						// 1: Editable text label
						val editableLabel = factories(EditableTextLabel)(new PointerWithEvents("Type Here"))
						editableLabel.addFocusListener(focusReporter("Label"))
						// 2: Button Row
						val buttonStack = factories(Stack).copy(axis = X, areRelated = true).build(Mixed) { factories =>
							// 2.1: Clear Button
							val clearButton = factories.mapContext { _ / Secondary }(TextButton)
								.apply("Clear (F1)", Set(HotKey.keyWithIndex(KeyEvent.VK_F1)),
									focusListeners = Vector(focusReporter("Clear Button"))) {
									editableLabel.text = ""
								}
							// 2.2: Close Button
							val closeButton = factories.mapContext { _ / Primary }(TextButton)
								.apply("Close (esc)", Set(HotKey.keyWithIndex(KeyEvent.VK_ESCAPE)),
									focusListeners = Vector(focusReporter("Close Button"))) {
									windowPointer.value.foreach { _.close() }
								}
							Vector(clearButton, closeButton)
						}
						Vector(editableLabel, buttonStack.parent)
					}
				}.parent
			
			Vector(framing, label2, editLabelFraming) -> label
		}
	}
	windowPointer.set(window)
	
	// Displays the window
	window.setToExitOnClose()
	window.display(centerOnParent = true)
	start()
	
	// Adds user interaction
	val label = window.result
	GlobalKeyboardEventHandler += KeyTypedListener { event: KeyTypedEvent => label.text += event.typedChar.toString }
	window.focusKeyStateHandler += KeyStateListener(KeyStateEvent.keyFilter(KeyEvent.VK_BACK_SPACE)) { _ =>
		label.text = label.text.string.drop(1).noLanguageLocalizationSkipped
	}
}
