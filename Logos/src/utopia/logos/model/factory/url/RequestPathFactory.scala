package utopia.logos.model.factory.url

import java.time.Instant

/**
  * Common trait for request path-related factories which allow construction with individual properties
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait RequestPathFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param domainId New domain id to assign
	  * @return Copy of this item with the specified domain id
	  */
	def withDomainId(domainId: Int): A
	
	/**
	  * @param path New path to assign
	  * @return Copy of this item with the specified path
	  */
	def withPath(path: String): A
}

