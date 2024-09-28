package utopia.flow.view.immutable.eventful

import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.Destiny.Sealed
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Switch
import utopia.flow.view.template.eventful.{Changing, Flag, OptimizedChanging}

import scala.util.Try

object OptimizedMultiMergeMirror
{
	/**
	  * Creates a mirror that tracks 0-n source pointers
	  * @param sources The tracked source pointers
	  * @param condition A condition that must be met for mirroring to occur (default = always mirroring)
	  * @param value A function that yields an up-to-date value for this mirror, based on the source pointer values
	  * @tparam A Type of mirroring results
	  * @return A new mirror
	  */
	def apply[A](sources: Iterable[Changing[_]], condition: Flag = AlwaysTrue)(value: => A) =
		new OptimizedMultiMergeMirror[A](sources.filterNot { _.isFixed }, condition)(value)
	/**
	  * Creates a mirror that tracks 0-n source pointers
	  * @param source1 The first tracked source pointer
	  * @param source2 The second tracked source pointer
	  * @param moreSources More tracked source pointers
	  * @param value A function that yields an up-to-date value for this mirror, based on the source pointer values
	  * @tparam A Type of mirroring results
	  * @return A new mirror
	  */
	def apply[A](source1: Changing[_], source2: Changing[_], moreSources: Changing[_]*)
	            (value: => A): OptimizedMultiMergeMirror[A] =
		apply(Pair(source1, source2) ++ moreSources)(value)
}

/**
 * A fusion of n different pointers. Optimizes pointer-listening.
 * @tparam R Type of merge results
  * @author Mikko Hilpinen
 * @since 27.9.2024
  *
  * @param sources Source pointers being listened to
  * @param condition Condition for listening to the specified sources (default = always true)
  * @param formResult A function which calculates an up-to-date value for this mirror,
  *                   based on the source pointer values
 */
class OptimizedMultiMergeMirror[R](sources: Iterable[Changing[_]], condition: Flag = AlwaysTrue)(formResult: => R)
	extends OptimizedChanging[R]
{
	// ATTRIBUTES   -----------------------------
	
	override implicit lazy val listenerLogger: Logger = sources.headOption match {
		case Some(source1) => source1.listenerLogger
		case None => SysErrLogger
	}
	
	private var _value: R = formResult
	// Boolean flag set to true while _value is not up-to-date.
	// Only used while this mirror is not being listened to (lazy mode)
	private val outOfDateFlag = Switch()
	
	// Listener used while this mirror is not actively being listened to
	// Detaches as soon as the out-of-date status has been updated
	private val lazyLazyListener: Lazy[ChangeListener[Any]] = Lazy {
		ChangeListener.onAnyChange {
			outOfDateFlag.set()
			detachLazyListener()
			Detach
		}
	}
	// Listener used while there are listeners attached to this mirror
	// Whenever one of the origin values changes, calculates a new result and fires change events, if appropriate
	private val lazyActiveListener = Lazy {
		ChangeListener.onAnyChange {
			val oldValue = _value
			val newValue = formResult
			
			if (newValue == oldValue)
				Continue
			else {
				_value = newValue
				Continue.and {
					fireEvent(Lazy { Some(ChangeEvent(oldValue, newValue)) }).foreach { effect => Try { effect() }.log }
				}
			}
		}
	}
	// A listener that swaps between active and lazy mode based on whether there are listeners attached to this pointer
	private lazy val modeChangeListener = ChangeListener[Boolean] { e =>
		// Case: First listener attached to this mirror => Starts actively tracking the source pointers
		if (e.newValue) {
			sources.foreach { source =>
				source.addListener(lazyActiveListener.value)
				lazyLazyListener.current.foreach(source.removeListener)
			}
			updateValue()
		}
		// Case: All listeners detached => Enters lazy mode
		else
			sources.foreach { source =>
				source.addListener(lazyLazyListener.value)
				lazyActiveListener.current.foreach(source.removeListener)
			}
	}
	
	
	// INITIAL CODE -----------------------------
	
	condition.addListener { conditionEvent =>
		// Case: This mirror was activated => Starts tracking mode & makes sure the value gets updated correctly
		if (conditionEvent.newValue) {
			val oldValue = _value
			hasListenersFlag.addListener(modeChangeListener)
			// Case: Being listened to => Fires a change event, if necessary
			if (hasListeners) {
				_value = formResult
				outOfDateFlag.reset()
				modeChangeListener.onChangeEvent(ChangeEvent(false, true))
				fireEventIfNecessary(oldValue).foreach { effect => Try { effect() }.log }
			}
			// Case: Not being listened to at the moment => Just marks the current value as deprecated
			else {
				modeChangeListener.onChangeEvent(ChangeEvent(true, false))
				outOfDateFlag.set()
			}
		}
		// Case: This mirror was deactivated => Stops tracking mode and stops listening to the source pointers
		else {
			hasListenersFlag.removeListener(modeChangeListener)
			updateValue()
			lazy val activeListeners = Pair(lazyLazyListener, lazyActiveListener).flatMap { _.current }
			sources.foreach { source => activeListeners.foreach(source.removeListener) }
		}
		Continue
	}
	// If active, sets up the initial state (lazy mode)
	if (condition.value)
		hasListenersFlag.addListenerAndSimulateEvent(true)(modeChangeListener)
	
	// Stops once all sources stop changing, or if the condition gets fixed to false
	stopOnceAllSourcesStop(sources)
	condition.onceFixedAt(false) { declareChangingStopped() }
	
	
	// IMPLEMENTED  -----------------------------
	
	override def value = {
		updateValue()
		_value
	}
	override def destiny: Destiny = {
		// Case: This mirror has been permanently deactivated => Sealed
		if (condition.isAlwaysFalse)
			Sealed
		// Case: Default => Calculates the state based on sources
		//                  & applies the possibility for sealing based on the condition
		else
			sources.map { _.destiny }.reduce { _ + _ }.possibleToSealIf(condition.destiny.isPossibleToSeal)
	}
	
	override def readOnly = this
	
	
	// OTHER    --------------------------------
	
	private def updateValue() = {
		// Checks whether the value needs an update
		val wasUpdated = outOfDateFlag.mutate { out =>
			// Case: Yes => Updates the managed value
			if (out) {
				_value = formResult
				true -> false
			}
			else
				false -> false
		}
		// If updated while on lazy mode, also attaches listeners to make sure the out-of-date state gets updated
		if (wasUpdated && hasNoListeners && condition.value)
			sources.foreach { _.addListener(lazyLazyListener.value) }
	}
	
	private def detachLazyListener() =
		lazyLazyListener.current.foreach { l => sources.foreach { _.removeListener(l) } }
}
