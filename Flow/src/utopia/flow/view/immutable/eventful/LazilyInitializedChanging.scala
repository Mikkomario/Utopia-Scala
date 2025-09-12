package utopia.flow.view.immutable.eventful

import utopia.flow.event.model.ChangeResult
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper, Flag}

object LazilyInitializedChanging
{
	/**
	  * @param initialize A lazily called function that initializes the wrapped pointer
	  * @tparam A Type of changing values
	  * @return A new lazily initialized changing wrapper
	  */
	def apply[A](initialize: => Changing[A]) = new LazilyInitializedChanging[A](Lazy(initialize))
	/**
	 * @param changing A lazy container that will yield a changing item
	 * @tparam A Type of the values in the changing item
	 * @return A lazily initialized wrapper for the changing item.
	 *         If the lazy container was already initialized, yields its contents.
	 */
	def apply[A](changing: Lazy[Changing[A]]): Changing[A] =
		changing.current.getOrElse(new LazilyInitializedChanging[A](changing))
}

/**
  * A changing item that's lazily initialized.
  *
  * Useful in situations where the pointer provider doesn't know whether the pointer will be actually utilized
  * (as initializing a pointer may cost resources and create additional dependencies to other pointers).
  *
  * @author Mikko Hilpinen
  * @since 19.12.2024, v2.5.1
  */
class LazilyInitializedChanging[+A](lazyWrapped: Lazy[Changing[A]]) extends ChangingWrapper[A]
{
	// ATTRIBUTES   ----------------------
	
	override protected lazy val wrapped: Changing[A] = lazyWrapped.value
	
	
	// IMPLEMENTED  ----------------------
	
	override implicit def listenerLogger: Logger = wrapped.listenerLogger
	
	override def readOnly: Changing[A] = this
	
	override def hasListeners: Boolean = lazyWrapped.current match {
		case Some(p) => p.hasListeners
		case None => false
	}
	override def numberOfListeners: Int = lazyWrapped.current match {
		case Some(p) => p.numberOfListeners
		case None => 0
	}
	
	override def toString = lazyWrapped.current match {
		case Some(wrapped) => s"$wrapped.initialized"
		case None => "Unitialized"
	}
	
	override def map[B](f: A => B): Changing[B] = mapWrapped { _.map(f) }
	override def lightMap[B](f: A => B): Changing[B] = mapWrapped { _.lightMap(f) }
	override def mapUntil[B](f: A => B)(stopCondition: B => Boolean): Changing[B] =
		mapWrapped { _.mapUntil(f)(stopCondition) }
	override def mapWithState[B](f: A => B): Changing[ChangeResult[B]] = mapWrapped { _.mapWithState(f) }
	override def lightMapWithState[B](f: A => B): Changing[ChangeResult[B]] = mapWrapped { _.lightMapWithState(f) }
	override def mapWithStateUntil[B](f: A => B)(stopCondition: (A, B) => Boolean): Changing[ChangeResult[B]] =
		mapWrapped { _.mapWithStateUntil(f)(stopCondition) }
	
	override def flatMap[B](f: A => Changing[B]): Changing[B] = mapWrapped { _.flatMap(f) }
	override def flatMapWhile[B](condition: => Flag)(f: A => Changing[B]): Changing[B] =
		mapWrapped { _.flatMapWhile(condition)(f) }
	
	override def viewUntil(f: A => Boolean): Changing[A] = mapWrapped { _.viewUntil(f) }
	override def viewWithStateUntil(f: A => Boolean): Changing[ChangeResult[A]] = mapWrapped { _.viewWithStateUntil(f) }
	
	
	// OTHER    ------------------------
	
	private def mapWrapped[B](f: Changing[A] => Changing[B]) = lazyWrapped.current match {
		case Some(wrapped) => f(wrapped)
		case None => LazilyInitializedChanging(lazyWrapped.map(f))
	}
}
