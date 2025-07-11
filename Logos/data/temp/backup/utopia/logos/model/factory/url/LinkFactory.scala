package utopia.logos.model.factory.url

import utopia.flow.generic.model.immutable.Model

import java.time.Instant

/**
  * Common trait for link-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
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
	  * @param pathId New path id to assign
	  * @return Copy of this item with the specified path id
	  */
	def withPathId(pathId: Int): A
	
	/**
	  * @param queryParameters New query parameters to assign
	  * @return Copy of this item with the specified query parameters
	  */
	def withQueryParameters(queryParameters: Model): A
}

