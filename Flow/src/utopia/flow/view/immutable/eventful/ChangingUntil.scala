package utopia.flow.view.immutable.eventful

import utopia.flow.async.process.Breakable
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.Sealed
import utopia.flow.operator.Identity
import utopia.flow.view.template.eventful.{Changing, OptimizedChanging}

import scala.concurrent.Future

object ChangingUntil
{
	/**
	  * Creates a stoppable view into a changing pointer
	  * @param origin A pointer to view
	  * @param stopCondition Condition that, when met, terminates origin pointer viewing / event generation
	  * @tparam A Type of viewed values
	  * @return A new temporary view into the specified pointer
	  */
	def apply[A](origin: Changing[A])(stopCondition: A => Boolean) =
		new ChangingUntil[A, A](origin, Identity, stopCondition)
	/**
	  * Creates a new stoppable mapping view into a changing pointer
	  * @param origin A pointer to map
	  * @param f A mapping function to apply
	  * @param stopCondition A condition that, when met, terminates the pointer mapping / event generation
	  * @tparam A Type of viewed values
	  * @tparam B Type of mapping results
	  * @return A new temporary mapping view into the specified pointer
	  */
	def map[A, B](origin: Changing[A])(f: A => B)(stopCondition: B => Boolean) =
		new ChangingUntil[A, B](origin, f, stopCondition)
}

/**
  * A changing wrapper that terminates the link at a certain point
  * @author Mikko Hilpinen
  * @since 14.11.2023, v2.3
  */
class ChangingUntil[-O, R](origin: Changing[O], f: O => R, stopCondition: R => Boolean)
	extends OptimizedChanging[R] with Breakable
{
	// ATTRIBUTES   --------------------
	
	private var stopped = false
	
	private val bridge: OptimizedBridge[O, R] = OptimizedBridge.map(origin, hasListenersFlag)(f) { eventView =>
		val effects = fireEvent(eventView)
		// May stop viewing
		if (eventView.value.exists { e => stopCondition(e.newValue) })
			stop()
		effects
	}
	
	
	// INITIAL CODE --------------------
	
	origin.addChangingStoppedListenerAndSimulateEvent { stop() }
	
	
	// IMPLEMENTED  --------------------
	
	override def value: R = bridge.value
	override def destiny: Destiny = if (stopped) Sealed else origin.destiny.possibleToSeal
	
	override def stop(): Future[Any] = {
		if (!stopped) {
			stopped = true
			declareChangingStopped()
			bridge.detach()
		}
		Future.successful(())
	}
}
