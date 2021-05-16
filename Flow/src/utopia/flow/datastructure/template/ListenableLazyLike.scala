package utopia.flow.datastructure.template

import utopia.flow.event.{ChangingLike, LazyListener}

import scala.concurrent.Future

/**
  * A common trait for lazy containers that generate events
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.9.2
  */
trait ListenableLazyLike[+A] extends LazyLike[A]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return A view to the state of this lazy, which also provides access to change events concerning this
	  *         instance's state
	  */
	def stateView: ChangingLike[Option[A]]
	
	/**
	  * @return A future of the first available value of this lazy container
	  */
	def valueFuture: Future[A]
	
	/**
	  * Adds a new listener to this lazy container
	  * @param listener A listener to be informed whenever new values are generated
	  */
	def addListener(listener: => LazyListener[A]): Unit
	/**
	  * Removes a listener from this lazy container
	  * @param listener A listener to no longer be informed about value generations
	  */
	def removeListener(listener: Any): Unit
	
	/**
	  * Creates a new container that lazily maps the value of this lazy
	  * @param f A mapping function
	  * @tparam B Mapping function result type
	  * @return A lazily initialized container based on lazily mapped contents of this lazy
	  */
	def map[B](f: A => B): ListenableLazyLike[B]
	
	
	// OTHER    -------------------------------
	
	/**
	  * Adds a new listener to be informed whenever new values are generated.
	  * If a value has already been generated, informs the listener about that immediately.
	  * @param listener A listener to inform of value generation events
	  */
	def addListenerAndSimulateEvent(listener: => LazyListener[A]) = current match
	{
		case Some(initialized) =>
			val l = listener
			addListener(l)
			l.onValueGenerated(initialized)
		case None => addListener(listener)
	}
}
