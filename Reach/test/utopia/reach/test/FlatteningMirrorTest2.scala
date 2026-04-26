package utopia.reach.test

import utopia.firmament.component.Window
import utopia.firmament.model.stack.StackSize
import utopia.flow.async.process.{Delay, Wait}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.{ChangeEvent, ChangeResponse}
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.AlwaysFalse
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.Flag
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
 *
 * @author Mikko Hilpinen
 * @since 12.03.2026, v
 */
object FlatteningMirrorTest2 extends App
{
	// TESTS    ----------------------
	
	private val popupP = Volatile.eventful.empty[Window]
	private val popupVisibleFlag: Flag = popupP.flatMap {
		case Some(window) =>
			println("Popup created => Visible flag = window visibility")
			println(s"\t- Window visible flag: ${ window.fullyVisibleFlag.hashCode() }: ${ window.fullyVisibleFlag }")
			/*
			window.fullyVisibleFlag.addLowPriorityListener { e =>
				println(s"Popup fully visible = ${ e.newValue }")
				throw new IllegalStateException("TEST")
			}*/
			window.fullyVisibleFlag
		case None => AlwaysFalse
	}
	
	println("Pointers:")
	println(s"\t- Popup pointer: ${ popupP.hashCode() }: $popupP")
	println(s"\t- Popup visible flag: ${ popupVisibleFlag.hashCode() }: $popupVisibleFlag")
	
	println("----")
	println("Starting listening")
	startListening()
	Wait(0.1.seconds)
	println("----")
	println("Displaying the window")
	private val window = show()
	
	Wait(2.seconds)
	println("----")
	println("Stopping")
	window.close()
	
	
	// OTHER    ---------------------------
	
	private def show() = {
		popupP.mutate {
			// Case: Pop-up already created => Displays it
			case Some(popup) =>
				if (!popup.visible)
					Delay(0.1.seconds) { popup.display() }
				popup -> Some(popup)
			
			// Case: No pop-up available yet => Creates and displays a new window
			case None =>
				val popup = createPopup()
				Delay(0.3.seconds) {
					println("----")
					println("Actually displaying the window")
					popup.display()
				} // FIXME: Display immediately (this delay is for testing)
				popup -> Some(popup)
		}
	}
	private def createPopup(): Window = {
		// Creates the pop-up
		val popup = ReachWindow.contentContextual
			.using(EmptyLabel) { (_, labelF) => labelF(StackSize.fixed(Size(300, 200))) }.window
		// Remembers when the pop-up closes
		/*
		popup.fullyVisibleFlag.addListener { e =>
			if (e.newValue)
				_lastPopupOpenTime = Now
			else
				_lastPopupCloseTime = Now
		}*/
		// Returns the pop-up window
		popup
	}
	
	private def startListening() = {
		println(s"Popup initially visible = ${ popupVisibleFlag.value }")
		Delay.after(1.seconds) { println(s"Popup visible after delay = ${ popupVisibleFlag.value }") }
		popupVisibleFlag.addListenerAndSimulateEvent(false)(VisibilityListener)
	}
	
	private object VisibilityListener extends ChangeListener[Boolean]
	{
		override def onChangeEvent(event: ChangeEvent[Boolean]): ChangeResponse = {
			println(s"POPUP VISIBLE = ${ event.newValue }")
		}
	}
}
