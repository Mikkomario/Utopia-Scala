package utopia.logos.model.factory.text

import java.time.Instant

/**
  * Common trait for word-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait WordFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param text New text to assign
	  * @return Copy of this item with the specified text
	  */
	def withText(text: String): A
}

