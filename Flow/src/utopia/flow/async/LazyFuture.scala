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
	
	/**
	  * @param f A function that will produce a future. Will be called when the result is first requested.
	  * @tparam A Type of eventual future result
	  * @return A new lazily started future
	  */
	def flatten[A](f: ExecutionContext => Future[A]) = new LazyFuture[A](f)
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
	def value(implicit executionContext: ExecutionContext) = cached match
	{
		case Some(v) => v
		case None =>
			val newFuture = generator(executionContext)
			cached = Some(newFuture)
			newFuture
	}
	/**
	  * @param executionContext An implicit execution context. Used only when requesting for a new value.
	  * @return Future of the eventual results
	  */
	@deprecated("Replaced with .value", "v1.17")
	def get(implicit executionContext: ExecutionContext) = value
	
	/**
	  * @return Currently cached results. None if the item hasn't been requested or received yet.
	  */
	def current = cached.flatMap { _.current }
}
