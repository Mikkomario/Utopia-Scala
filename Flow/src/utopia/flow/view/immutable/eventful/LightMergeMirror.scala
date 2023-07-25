package utopia.flow.view.immutable.eventful

import utopia.flow.collection.immutable.Pair
import utopia.flow.view.immutable.View
import utopia.flow.view.template.eventful.{Changing, OptimizedChanging}

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
		new LightMergeMirror[O1, O2, R](origin1, origin2, merge)
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
class LightMergeMirror[O1, O2, R](origin1: Changing[O1], origin2: Changing[O2], merge: (O1, O2) => R)
	extends OptimizedChanging[R]
{
	// ATTRIBUTES   -------------------
	
	private val input1 = new Input(origin1, merge(_, origin2.value))
	private val input2 = new Input(origin2, merge(origin1.value, _))
	private val inputs = Pair(input1, input2)
	
	
	// IMPLEMENTED  -------------------
	
	override def value: R = merge(input1.value, input2.value)
	override def isChanging: Boolean = inputs.exists { _.origin.isChanging }
	
	
	// NESTED   ----------------------
	
	private class Input[O](val origin: Changing[O], transform: O => R) extends View[O]
	{
		private val bridge = OptimizedBridge(origin, hasListenersFlag) { e =>
			e.value match {
				// Case: Update (expected) => Updates the merge result and fires a change event, if appropriate
				case Some(event) => fireEventIfNecessary(transform(event.oldValue), transform(event.newValue))
				// Case: No update (unexpected) => No-op
				case None => Vector()
			}
		}
		override def value: O = bridge.value
	}
}
