package utopia.reach.test.interactive

import utopia.flow.collection.immutable.Single
import utopia.flow.view.immutable.eventful.AlwaysFalse
import utopia.genesis.handling.event.consume.ConsumeChoice.Consume
import utopia.genesis.handling.event.keyboard.Key.Enter
import utopia.genesis.handling.event.mouse.MouseButtonStateListener
import utopia.reach.component.interactive.input.FieldWithPopup
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.focus.Focusable
import utopia.reach.container.wrapper.Framing
import utopia.reach.focus.FocusRequestable
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
 * Tests the field-with-popup -feature
 * @author Mikko Hilpinen
 * @since 15.09.2025, v1.7
 */
object FieldWithPopupTest extends App
{
	private val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		framingF.build(FieldWithPopup) { fieldF =>
			val field = fieldF.withActivationKey(Enter).withPopupContext(windowContext.borderless)
				.apply(AlwaysFalse) { fieldContext =>
					Focusable.wrap(fieldContext(ViewTextLabel).text("Click me"), Single(fieldContext.focusListener))
				} { _(ViewTextLabel).text("More text here") }
			
			field.mouseButtonHandler += MouseButtonStateListener.leftPressed.over(field.bounds).apply { _ =>
				field.showPopup()
				Consume("Showing pop-up")
			}
			
			field
		}
	}
	
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	start()
}
