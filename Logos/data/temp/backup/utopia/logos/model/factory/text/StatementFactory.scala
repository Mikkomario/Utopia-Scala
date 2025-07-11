package utopia.logos.model.factory.text

import java.time.Instant

/**
  * Common trait for statement-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
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

