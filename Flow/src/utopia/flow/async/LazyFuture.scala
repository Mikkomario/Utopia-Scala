package utopia.flow.async

import AsyncExtensions._

import scala.concurrent.{ExecutionContext, Future}

object LazyFuture
{
	/**
	  * @param f A function that will be called asynchronously when new results are required
	  * @tparam A Type of function result
	  * @return An access point to a lazily initialized future that uses the specified function
	  */
	def apply[A](f: => A) = new LazyFuture[A](e => Future(f)(e))
}

/**
  * A lazily initialized asynchronous access point
  * @author Mikko Hilpinen
  * @since 18.7.2020, v1.8
  * @param generator A function for producing asynchronous results
  */
class LazyFuture[A](generator: ExecutionContext => Future[A])
{
	// ATTRIBUTES	--------------------------
	
	private var cached: Option[Future[A]] = None
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @param executionContext An implicit execution context. Used only when requesting for a new value.
	  * @return Future of the eventual results
	  */
	def get(implicit executionContext: ExecutionContext) = cached match
	{
		case Some(v) => v
		case None =>
			val newFuture = generator(executionContext)
			cached = Some(newFuture)
			newFuture
	}
	
	/**
	  * @return Currently cached results. None if the item hasn't been requested or received yet.
	  */
	def current = cached.flatMap { _.current.flatMap { _.toOption } }
	
	
	// OTHER	----------------------------
	
	/**
	  * Resets this container so that a new request will be made at the next call of .get
	  */
	def reset() = cached = None
}
