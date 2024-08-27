package utopia.flow.view.mutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.MaybeEmpty
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.eventful.{EventfulPointer, LockablePointer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

trait PointerFactory[+P[_]]
{
	// ABSTRACT ----------------------------
	
	/**
	  * Creates a new pointer
	  * @param initialValue Value placed in this pointer, initially
	  * @tparam A Type of values held within this pointer
	  * @return A new pointer
	  */
	def apply[A](initialValue: A): P[A]
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @tparam A Type of held value, when defined
	  * @return A new empty pointer
	  */
	def empty[A] = optional[A]()
	/**
	  * @tparam A Type of individually held values
	  * @return A new empty pointer that may contain 0-n values
	  */
	def emptySeq[A] = seq[A]()
	
	
	// OTHER    ----------------------------
	
	/**
	  * Creates a new pointer that contains an optional value
	  * @param value The initial value of this pointer (default = None)
	  * @tparam A Type of values in this pointer, when specified
	  * @return A new option pointer
	  */
	def optional[A](value: Option[A] = None) = apply[Option[A]](value)
	/**
	  * Creates a pointer that contains 0-n values
	  * @param initialValue Values that are placed in this pointer, initially (default = empty)
	  * @tparam A Type of individual values
	  * @return A new pointer
	  */
	def seq[A](initialValue: Seq[A] = Empty) = apply(initialValue)
}

object Pointer extends PointerFactory[Pointer]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return A factory for constructing pointers that fire change events
	  */
	def eventful = EventfulPointer
	/**
	  * @return A factory for constructing pointers that allow locking
	  */
	def lockable = LockablePointer
	/**
	  * @return A factory for constructing pointers that only weakly reference their contents after some time
	  */
	def releasing = ReleasingPointer
	
	
	// OTHER    ----------------------------
	
	/**
	  * Creates a new pointer
	  */
	def apply[A](value: A): Pointer[A] = new _Pointer(value)
	
	@deprecated("Renamed to .optional(Option)", "v2.5")
	def option[A](value: Option[A] = None) = apply[Option[A]](value)
	
	/**
	  * Creates a new pointer with events
	  * @param value The initial value for the pointer
	  * @tparam A The type of the contained item
	  * @return A new pointer with events
	  */
	@deprecated("Renamed to .eventful(A)", "v2.2")
	def withEvents[A](value: A)(implicit log: Logger) = EventfulPointer(value)
	
	/**
	  * @param referenceDuration Duration of strong references to the held values
	  * @param initialValue Initially held value (optional)
	  * @param exc               Implicit execution context
	  * @tparam A Type of held value, when defined
	  * @return A new pointer that switches to a weak reference after a while
	  */
	@deprecated("Deprecated for removal. Please use .releasing.after(...) instead", "v2.5")
	def releasingAfter[A <: AnyRef](referenceDuration: Duration, initialValue: Option[A] = None)
	                               (implicit exc: ExecutionContext) =
		ReleasingPointer.after[A](referenceDuration, initialValue)
	
	
	// EXTENSIONS --------------------------
	
	implicit class OptionPointer[A](val p: Pointer[Option[A]])
		extends AnyVal with MaybeEmpty[Pointer[Option[A]]] with Resettable
	{
		// COMPUTED ------------------------
		
		def value_=(newValue: A) = p.value = Some(newValue)
		
		
		// IMPLEMENTED  --------------------
		
		override def self: Pointer[Option[A]] = p
		
		override def isEmpty: Boolean = p.value.isEmpty
		override def isSet: Boolean = nonEmpty
		
		override def reset(): Boolean = p.mutate { _.isDefined -> None }
		
		
		// OTHER    ------------------------
		
		/**
		  * @param value Value to assign to this pointer
		  * @return The assigned value
		  */
		def setOne(value: A) = {
			p.value = Some(value)
			value
		}
		/**
		  * @param value Value that will be placed in this pointer, if the current value is empty (call-by-name)
		  * @return Value after this update (either the previous or the current)
		  */
		def setOneIfEmpty(value: => A) = p.mutate {
			case old@Some(oldValue) => oldValue -> old
			case None =>
				val newValue = value
				newValue -> Some(newValue)
		}
		
		/**
		  * @param value New value to assign, if this pointer is currently empty
		  * @return value after this update
		  */
		def setIfEmpty(value: => Option[A]) = p.setIf { _.isEmpty }(value)
		
		/**
		  * Removes the current value from this pointer
		  */
		def clear() = p.value = None
		/**
		  * @return Removes and returns the current value from this pointer
		  */
		def pop() = p.getAndSet(None)
		
		/**
		 * Preserves the item in this pointer only if it satisfies the specified condition
		 * @param f A filter function
		 * @return This pointer
		 */
		def filterCurrent(f: A => Boolean) = {
			p.update { v => v.filter(f) }
			p
		}
		/**
		 * Removes the item from this pointer if it satisfies the specified condition
		 * @param f A filter function
		 * @return This pointer
		 */
		def filterNotCurrent(f: A => Boolean) = filterCurrent { !f(_) }
		
		/**
		  * Updates the value of this pointer if there is no value already, returns the value after the update
		  * @param v Value to assign if there is no value in this pointer (call by name)
		  * @return Existing pointer value or the assigned value
		  */
		def getOrElseUpdate(v: => A) = p.value.getOrElse {
			val newValue = v
			p.value = Some(newValue)
			newValue
		}
		
		/**
		 * Updates the current value in this pointer, but only if defined
		 * @param f A mapping function that applies to the current value only
		  * @return Value in this pointer after this update
		 */
		def mapCurrent(f: A => A) = p.updateAndGet { _.map(f) }
	}
	
	implicit class SeqPointer[A](val p: Pointer[Seq[A]]) extends AnyVal with MaybeEmpty[Pointer[Seq[A]]]
	{
		// IMPLEMENTED  --------------------
		
		override def self: Pointer[Seq[A]] = p
		
		override def isEmpty: Boolean = p.value.isEmpty
		
		
		// OTHER    -------------------------
		
		/**
		  * Removes all values from this pointer
		  */
		def clear() = p.value = Empty
		
		/**
		  * @return Removes and returns the first value in this pointer
		  */
		def pop(): Option[A] = p.mutate { v => if (v.isEmpty) None -> v else Some(v.head) -> v.tail }
		/**
		  * @return Removes and returns the last value in this pointer
		  */
		def popLast() = p.mutate { v => v.lastOption -> v.dropRight(1) }
		/**
		  * Clears all items from this pointer
		  * @return Value before this method call
		  */
		def popAll() = p.getAndSet(Empty)
		
		/**
		  * Finds and removes an item from this pointer's contents
		  * @param f A function for finding the targeted item
		  * @return Item that was removed from this pointer. None if no match was found (i.e. no item was removed)
		  */
		def findAndPop(f: A => Boolean) = p.mutate[Option[A]] { coll =>
			coll.findIndexWhere(f) match {
				case Some(index) => Some(coll(index)) -> (coll.take(index) ++ coll.drop(index + 1))
				case None => None -> coll
			}
		}
		
		/**
		  * Adds a new item to the end of this list
		  */
		def :+=(item: A) = p.update { _ :+ item }
		/**
		  * Adds a new item to the beginning of this list
		  */
		def +:=(item: A) = p.update { _.+:(item) }
		
		/**
		  * Adds multiple new items to this list
		  */
		def ++=(items: IterableOnce[A]) = p.update { _ ++ items }
		
		/**
		  * Removes an item from this list
		  */
		def -=(item: Any) = p.update { _ filterNot { _ == item } }
		/**
		  * Removes multiple items from this list
		  */
		def --=(items: Iterable[Any]) = p.update { _ filterNot { my => items.exists(_ == my) } }
	}
	
	
	// NESTED   ---------------------------
	
	private class _Pointer[A](override var value: A) extends Pointer[A]
}

/**
  * A common trait for wrapper classes which allow direct mutation and replacement of the wrapped value
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  */
trait Pointer[A] extends Any with View[A]
{
	// ABSTRACT	----------------------------
	
	/**
	  * Updates the value in this wrapper
	  * @param newValue The new value to assign to this wrapper
	  */
	def value_=(newValue: A): Unit
	
	
	// OTHER	----------------------------
	
	/**
	  * Mutates or modifies the value in this wrapper
	  * @param f A mapping / modifying function applied to the current value of this wrapper. The value returned by
	  *          this function is assigned to this wrapper.
	  */
	def update(f: A => A) = value = f(value)
	
	/**
	  * Mutates the value in this wrapper, also returning an alternative result
	  * @param mutate A mutator function that returns the final result and the new value
	  * @tparam B Type of the additional result
	  * @return The additional result returned by the mutator function
	  */
	def mutate[B](mutate: A => (B, A)) = {
		val (result, newValue) = mutate(value)
		value = newValue
		result
	}
	
	/**
	  * Updates the contents of this pointer and returns the value after the update
	  * @param f A function for modifying the contents of this pointer
	  * @return Value after the update
	  */
	def updateAndGet(f: A => A) = {
		update(f)
		value
	}
	/**
	  * Updates the contents of this pointer and returns the value before the update
	  * @param f A function for modifying the contents of this pointer
	  * @return Value before the update
	  */
	def getAndUpdate(f: A => A) = {
		val result = value
		value = f(result)
		result
	}
	/**
	  * Updates the contents of this pointer. Returns the state before the update.
	  * @param newValue New value to assign to this pointer.
	  * @return Previous value of this pointer.
	  */
	def getAndSet(newValue: A) = {
		val result = value
		value = newValue
		result
	}
	
	/**
	  * Updates the value of this pointer, but only if the current value satisfies the specified condition
	  * @param condition Condition applied to the current pointer value
	  * @param newValue Value assigned if the current value satisfies the specified condition (call-by-name)
	  * @return Value in this pointer after this update
	  */
	def setIf(condition: A => Boolean)(newValue: => A) =
		updateAndGet { current => if (condition(current)) newValue else current }
	/**
	  * Updates the value in this container, but only if the current value satisfies the specified condition
	  * @param condition A condition for updating. Applied to this pointer's current value.
	  * @param mutate A mutating function (only called if condition applies)
	  * @return Value of this pointer after this update
	  */
	def updateIf(condition: A => Boolean)(mutate: A => A) = updateAndGet { v => if (condition(v)) mutate(v) else v }
}
