package utopia.annex.model

import utopia.flow.async.TryFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.template.ModelConvertible

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
	type RequestQueueable = Either[ApiRequestSeed, ApiRequest]
	
	/**
	  * A type that represents an API-request in either prepared or "seed" form
	  */
	type RequestQueueable2[A] = Either[ApiRequestSeed2[A], ApiRequest2[A]]
	
	@deprecated("This type will be rewritten in a future release. Please use PostSpiritRequest instead", "v1.7")
	type PostRequest[+S <: Spirit with ModelConvertible] = PostSpiritRequest[S]
	
	
	// IMPLICIT ----------------------
	
	implicit class RichRequestQueueable(val q: RequestQueueable) extends AnyVal with Retractable
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
