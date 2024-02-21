package utopia.reach.test

import utopia.firmament.component.Window
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalString._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.view.mutable.eventful.{EventfulPointer, SettableOnce}
import utopia.genesis.handling.event.keyboard.Key.BackSpace
import utopia.genesis.handling.event.keyboard.{KeyStateListener2, KeyTypedListener2, KeyboardEvents}
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorRole.{Primary, Secondary}
import utopia.paradigm.color.ColorShade.Light
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.button.image.ImageAndTextButton
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
				// Hello Label (Primary)
				.build(MutableTextLabel) { _.withHorizontallyCenteredText.withBackground(Primary)("Hello!") }.toTuple
			// 2: Hello Label 2
			val label2 = factories(TextLabel).withTextAlignment(Alignment.Center)
				.apply("Hello 2\nThis label contains 2 lines")
			// 3: Framing
			val editLabelFraming = factories(Framing).withBackground(Primary, Light).build(Stack) { stackF =>
					// Column (Centered)
					stackF.centered.build(Mixed) { factories =>
						// 1: Editable text label
						val editableLabel = factories(EditableTextLabel)
							.withFocusListener(focusReporter("Label"))(new EventfulPointer("Type Here"))
						// 2: Button Row
						val buttonStack = factories(Stack).related.row.build(Mixed) { factories =>
							// 2.1: Clear Button
							val clearButton = factories.mapContext { _ / Secondary }(TextButton)
								.withFocusListener(focusReporter("Clear Button"))
								.triggeredWithKeyIndex(KeyEvent.VK_F1)
								.apply("Clear (F1)") { editableLabel.text = "" }
							// 2.2: Close Button
							val closeButtonFactory = factories.mapContext { _ / Primary }(ImageAndTextButton)
								.withFocusListener(focusReporter("Close Button"))
								.triggeredWithKeyIndex(KeyEvent.VK_ESCAPE)
							val closeButton = closeButtonFactory.sizeChanging
								.apply(SingleColorIcon(
									Image.readFrom("Reach/test-images/close.png").getOrElse(Image.empty).cropped),
									"Close (esc)") { windowPointer.value.foreach { _.close() } }
							/*
							val closeButton = factories.mapContext { _ / Primary }(TextButton)
								.withFocusListener(focusReporter("Close Button"))
								.triggeredWithKeyIndex(KeyEvent.VK_ESCAPE)
								.apply("Close (esc)") { windowPointer.value.foreach { _.close() } }
							 */
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
	KeyboardEvents += KeyTypedListener2.unconditional { event => label.text += event.typedChar.toString }
	window.focusKeyStateHandler += KeyStateListener2(BackSpace) { _ =>
		label.text = label.text.string.drop(1).noLanguageLocalizationSkipped
	}
}
