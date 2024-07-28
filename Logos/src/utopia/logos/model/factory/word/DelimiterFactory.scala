package utopia.logos.model.factory.word

import java.time.Instant

/**
  * Common trait for delimiter-related factories which allow construction with individual properties
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait DelimiterFactory[+A]
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

