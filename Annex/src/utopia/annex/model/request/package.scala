package utopia.annex.model

import utopia.flow.async.TryFuture
import utopia.flow.collection.CollectionExtensions._

/**
  * @author Mikko Hilpinen
  * @since 21.12.2023, v1.6.1
  */
package object request
{
	// TYPES    ----------------------
	
	/**
	  * A type that represents an API-request in either prepared or "seed" form
	  */
	type RequestQueueable[+A] = Either[ApiRequestSeed[A], ApiRequest[A]]
	
	
	// IMPLICIT ----------------------
	
	implicit class RichRequestQueueable[+A](val q: RequestQueueable[A]) extends AnyVal with Retractable
	{
		// COMPUTED ------------------
		
		/**
		  * @return A future that resolves into a prepared request. May fail.
		  */
		def toRequest = q match {
			case Left(seed) => seed.toRequest
			case Right(prepared) => TryFuture.success(prepared)
		}
		
		
		// IMPLEMENTED  --------------
		
		override def deprecated: Boolean = q.either.deprecated
	}
}
