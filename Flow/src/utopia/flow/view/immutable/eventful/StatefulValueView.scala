package utopia.flow.view.immutable.eventful

import utopia.flow.event.model.{ChangeEvent, ChangeResult, Destiny}
import utopia.flow.operator.Identity
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.eventful.{Changing, OptimizedChanging}

object StatefulValueView
{
	/**
	  * Creates a new stateful view into a changing pointer
	  * @param origin Pointer to view
	  * @tparam A Type of values in the original pointer
	  * @return A pointer that attaches the "will change" -status to the original pointer's values
	  */
	def apply[A](origin: Changing[A]) =
		new StatefulValueView[A, A](origin, Identity, cachingDisabled = true)
	/**
	  * Creates a new stateful and mapping view into a pointer
	  * @param origin Pointer to view / map
	  * @param cachingDisabled Whether map result caching should be disabled.
	  *                        Only set this true in cases where the mapping function is very simple.
	  * @param f A mapping function that transforms the origin's values
	  * @tparam A Type of original values
	  * @tparam B Type of mapped values
	  * @return A pointer that attaches the "will change" -status to mapped pointer values
	  */
	def map[A, B](origin: Changing[A], cachingDisabled: Boolean = false)(f: A => B) =
		new StatefulValueView[A, B](origin, _.mapValue(f), cachingDisabled = cachingDisabled)
	/**
	  * Creates a new temporary and stateful view into a pointer
	  * @param origin Pointer to view
	  * @param stopCondition A condition that, when met, will end origin pointer mapping.
	  *                      Accepts each origin pointer value.
	  * @tparam A Type of origin pointer's values
	  * @return A new temporary view
	  */
	def stopIf[A](origin: Changing[A])(stopCondition: A => Boolean) =
		new StatefulValueView[A, A](origin, v => if (v.isFinal || !stopCondition(v)) v else v.asFinal,
			mayStopMapping = true)
	/**
	  * Creates a new temporary mapping view into a pointer
	  * @param origin Pointer to map
	  * @param f A mapping function applied to origin pointer's values
	  * @param stopCondition A condition that, if met, terminates origin pointer mapping / viewing and makes
	  *                      this pointer static.
	  *                      Accepts the origin pointer's value, as well as the mapped value.
	  * @tparam A Type of original values
	  * @tparam B Type of map results
	  * @return A new temporary mapping view
	  */
	def mapAndStopIf[A, B](origin: Changing[A])(f: A => B)(stopCondition: (A, B) => Boolean) =
		new StatefulValueView[A, B](origin,
			v =>
				if (v.isFinal)
					v.mapValue(f)
				else {
					val mapped = f(v.value)
					ChangeResult(mapped, stopCondition(v, mapped))
				},
			mayStopMapping = true)
}

/**
  * A view into a [[Changing]] pointer that wraps the values in a [[ChangeResult]]
  * @author Mikko Hilpinen
  * @since 14.11.2023, v2.3
  *
  * @tparam O Type of the origin / source values
  * @tparam R Type of the mapped values
  */
class StatefulValueView[-O, R](origin: Changing[O], f: ChangeResult[O] => ChangeResult[R],
                               mayStopMapping: Boolean = false, cachingDisabled: Boolean = false)
	extends OptimizedChanging[ChangeResult[R]]
{
	// ATTRIBUTES   ---------------------
	
	private val bridge: OptimizedBridge[O, ChangeResult[R]] = OptimizedBridge
		.map(origin, hasListenersFlag, cachingDisabled) { value =>
			if (origin.isFixed) f(ChangeResult.finalValue(value)) else f(value)
		} { eventView =>
			val effects = fireEvent(eventView)
			// If the mapping caused the value to get fixed, ends mapping & declares changing as stopped
			if (mayStopMapping && eventView.value.exists { _.newValue.isFinal })
				stopChanging()
			effects
		}
	
	origin.onceChangingStops {
		// May update the final value
		val rawValue = bridge.value
		if (rawValue.isTemporal)
			fireEvent(Lazy { Some(ChangeEvent(rawValue, rawValue.asFinal)) }).foreach { _() }
		// Stops changing once the source stops changing
		stopChanging()
	}
	
	
	// IMPLEMENTED  ---------------------
	
	override implicit def listenerLogger: Logger = origin.listenerLogger
	
	// Seals (stops changing) if maps to a final value
	override def destiny: Destiny =
		origin.destiny.sealedIf { mayStopMapping && bridge.value.isTemporal }.possibleToSealIf(mayStopMapping)
	
	override def value: ChangeResult[R] = {
		val raw = bridge.value
		if (origin.isFixed) raw.asFinal else raw
	}
	
	override def readOnly = this
	
	
	// OTHER    ------------------------
	
	private def stopChanging(): Unit = {
		bridge.detach()
		declareChangingStopped()
	}
}
