package utopia.logos.model.factory.url

import utopia.flow.util.UncertainBoolean

import java.time.Instant

/**
  * Common trait for domain-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait DomainFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param isHttps New is https to assign
	  * @return Copy of this item with the specified is https
	  */
	def withIsHttps(isHttps: UncertainBoolean): A
	
	/**
	  * @param url New url to assign
	  * @return Copy of this item with the specified url
	  */
	def withUrl(url: String): A
}

