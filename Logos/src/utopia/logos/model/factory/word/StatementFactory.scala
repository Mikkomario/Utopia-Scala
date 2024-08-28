package utopia.logos.model.factory.word

import java.time.Instant

/**
  * Common trait for statement-related factories which allow construction with individual properties
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
trait StatementFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param delimiterId New delimiter id to assign
	  * @return Copy of this item with the specified delimiter id
	  */
	def withDelimiterId(delimiterId: Int): A
}

