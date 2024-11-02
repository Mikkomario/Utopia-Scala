package utopia.flow.view.mutable.caching

import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.async.VolatileSwitch
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.mutable.{Pointer, Resettable, Switch}

import java.time.Instant
import scala.annotation.unchecked.uncheckedVariance
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object LazilyUpdating
{
	/**
	  * @param initialValue Initially stored value in this container
	  * @param invalidatesAfter Time after which this container's contents should be considered
	  *                         deprecated / invalidated.
	  *                         Invalidated values are automatically updated upon a call to refresh() or value.
	  * @param update A function which updates the value held in this container.
	  *               Accepts:
	  *                     1. Currently held value
	  *                     1. Time when the currently held value was generated
	  *
	  *               Yields a new value to cache.
	  * @param log Implicit logging implementation used in pointer event handling
	  *            and for catching errors thrown by 'update'.
	  * @tparam A Type of values held in this container
	  * @return A new lazily updating container
	  */
	def apply[A](initialValue: A, invalidatesAfter: Duration = Duration.Inf)
	            (update: (A, Instant) => A)(implicit log: Logger) =
		new LazilyUpdating[A](initialValue, invalidatesAfter)(update)
}

/**
  * Common interface for lazily updating containers
  * @author Mikko Hilpinen
  * @since 01.11.2024, v2.5.1
  */
class LazilyUpdating[+A](initialValue: A, invalidatesAfter: Duration = Duration.Inf)
                        (update: (A, Instant) => A)(implicit log: Logger)
	extends View[A] with Resettable
{
	// ATTRIBUTES   --------------------------
	
	private val finiteInvalidation = invalidatesAfter.finite
	
	// Contains the previous update value
	// Variance here is ignored, as only values yielded by 'update' are stored here (all <: A)
	private val _pointer: EventfulPointer[(A @uncheckedVariance, Instant)] =
		Pointer.eventful(initialValue -> Now.toInstant)
	// Set to true on manual invalidation => Triggers an incremental update at the next request to value
	private val invalidatedFlag = Switch()
	
	// Flags for making sure the initialization or updating process doesn't trigger itself
	private val updatingFlag = VolatileSwitch()
	
	/**
	  * A pointer that contains the latest value of this updating container.
	  * Does not update in real time, but only when this container's [[value]] is called.
	  */
	lazy val pointer = _pointer.map { _._1 }
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The currently stored value in this container
	  */
	def current = pointer.value
	
	
	// IMPLEMENTED  ----------------------
	
	override def value: A = refresh()
	override def isSet: Boolean = invalidatedFlag.isNotSet &&
		finiteInvalidation.forall { d => _pointer.value._2 > Now - d }
	
	/**
	  * Invalidates this container, so that the next call on [[value]] or [[refresh]] will trigger an update
	  * @return Whether this item's state changed
	  */
	override def reset(): Boolean = if (updatingFlag.isSet || isNotSet) false else invalidatedFlag.set()
	
	
	// OTHER    ----------------------
	
	/**
	  * Refreshes this container, updating the held value if necessary.
	  * @return The value of this container after this refresh.
	  */
	def refresh() = {
		val now = Now.toInstant
		_pointer.mutate { cached =>
			// Case: The cached value has been invalidated => Incrementally updates it
			if ((invalidatedFlag.reset() || finiteInvalidation.exists { now - _ > cached._2 }) && updatingFlag.set()) {
				val newValue = Try { update(cached._1, cached._2) } match {
					case Success(v) => v -> now
					case Failure(error) =>
						log(error, "Update failed")
						cached
				}
				updatingFlag.reset()
				newValue._1 -> newValue
			}
			// Case: The cached value is up-to-date => Yields it
			else
				cached._1 -> cached
		}
	}
}
