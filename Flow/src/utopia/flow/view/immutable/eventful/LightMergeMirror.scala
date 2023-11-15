package utopia.flow.view.immutable.eventful

import utopia.flow.async.process.Breakable
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.Sealed
import utopia.flow.view.immutable.View
import utopia.flow.view.template.eventful.{Changing, OptimizedChanging}

import scala.concurrent.Future

object LightMergeMirror
{
	/**
	  * Creates a new pointer that merges the values from two other pointers
	  * @param origin1 The first merge origin
	  * @param origin2 Second merge origin
	  * @param merge   A function for producing a merge result based on the two origin values
	  * @tparam O1 Type of the first merge origin
	  * @tparam O2 Type of the second merge origin
	  * @tparam R  Type of merge results
	  * @return A new pointer
	  */
	def apply[O1, O2, R](origin1: Changing[O1], origin2: Changing[O2])(merge: (O1, O2) => R) =
		new LightMergeMirror[O1, O2, R](origin1, origin2, merge, None)
	/**
	  * Creates a new pointer that merges the values from two other pointers, until a certain condition is met
	  * @param origin1 The first merge origin
	  * @param origin2 Second merge origin
	  * @param merge   A function for producing a merge result based on the two origin values
	  * @param stopCondition A condition that, once met, causes this pointer to stop reflecting the values of the
	  *                      original pointers.
	  *                      Accepts:
	  *                         1. Source pointer 1 value
	  *                         1. Source pointer 2 value
	  *                         1. Merge result
	  * @tparam O1 Type of the first merge origin
	  * @tparam O2 Type of the second merge origin
	  * @tparam R  Type of merge results
	  * @return A new pointer
	  */
	def until[O1, O2, R](origin1: Changing[O1], origin2: Changing[O2])(merge: (O1, O2) => R)
	                    (stopCondition: (O1, O2, R) => Boolean) =
		new LightMergeMirror[O1, O2, R](origin1, origin2, merge, Some(stopCondition))
}

/**
  * A variant of the [[MergeMirror]] class which doesn't cache values and listens to the origin pointers as
  * little as possible.
  *
  * This version is more suitable for situations where the 'merge' function is very cheap,
  * and where continuous listening may be a problem (e.g. when combining with other optimizing pointers)
  *
  * @author Mikko Hilpinen
  * @since 25.7.2023, v2.2
  *
  * @tparam O1 Type of the first merge origin
  * @tparam O2 Type of the second merge origin
  * @tparam R Type of merge results
  *
  * @constructor Creates a new merging mirror pointer
  * @param origin1 The first merge origin
  * @param origin2 Second merge origin
  * @param merge A function for producing a merge result based on the two origin values
  */
class LightMergeMirror[O1, O2, R](origin1: Changing[O1], origin2: Changing[O2], merge: (O1, O2) => R,
                                  stopCondition: Option[(O1, O2, R) => Boolean])
	extends OptimizedChanging[R] with Breakable
{
	// ATTRIBUTES   -------------------
	
	private val input1 = new Input(origin1, merge(_, origin2.value))
	private val input2 = new Input(origin2, merge(origin1.value, _))
	private val inputs = Pair(input1, input2)
	
	private var stopped = false
	
	
	// INITIAL CODE -------------------
	
	// Handles the situation where the inputs stop from changing (if possible)
	onceAllSourcesStop(inputs.map { _.origin }) { stop() }
	
	
	// IMPLEMENTED  -------------------
	
	override def value: R = merge(input1.value, input2.value)
	
	override def destiny: Destiny = {
		// If stopped, it is certain that this mirror won't change
		if (stopped)
			Sealed
		// Otherwise the changing is based on the origin pointers
		// If a stop condition is specified, it brings a level of uncertainty (i.e. may always change)
		else
			inputs.mapAndMerge { _.origin.destiny } { _ + _ }.possibleToSealIf(stopCondition.isDefined)
	}
	
	override def stop(): Future[Any] = {
		if (!stopped) {
			stopped = true
			declareChangingStopped()
			inputs.foreach { _.bridge.detach() }
		}
		Future.successful(())
	}
	
	
	// NESTED   ----------------------
	
	private class Input[O](val origin: Changing[O], transform: O => R) extends View[O]
	{
		val bridge = OptimizedBridge(origin, hasListenersFlag) { e =>
			e.value match {
				// Case: Update (expected) => Updates the merge result and fires a change event, if appropriate
				case Some(event) =>
					val newMergeResult = transform(event.newValue)
					val afterEffects = fireEventIfNecessary(transform(event.oldValue), newMergeResult)
					// Tests whether should stop changing
					if (stopCondition.exists { _(input1.value, input2.value, newMergeResult) })
						stop()
					afterEffects
				
				// Case: No update (unexpected) => No-op
				case None => Vector()
			}
		}
		override def value: O = bridge.value
	}
}
