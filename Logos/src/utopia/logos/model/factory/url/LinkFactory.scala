package utopia.logos.model.factory.url

import utopia.flow.generic.model.immutable.Model

import java.time.Instant

/**
  * Common trait for link-related factories which allow construction with individual properties
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
trait LinkFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param queryParameters New query parameters to assign
	  * @return Copy of this item with the specified query parameters
	  */
	def withQueryParameters(queryParameters: Model): A
	
	/**
	  * @param requestPathId New request path id to assign
	  * @return Copy of this item with the specified request path id
	  */
	def withRequestPathId(requestPathId: Int): A
}

