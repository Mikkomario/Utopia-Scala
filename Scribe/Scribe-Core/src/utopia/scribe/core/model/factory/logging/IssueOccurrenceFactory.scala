package utopia.scribe.core.model.factory.logging

import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.model.immutable.Model

import java.time.Instant

/**
  * Common trait for issue occurrence-related factories which allow construction with individual 
  * properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait IssueOccurrenceFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param caseId New case id to assign
	  * @return Copy of this item with the specified case id
	  */
	def withCaseId(caseId: Int): A
	/**
	  * @param count New count to assign
	  * @return Copy of this item with the specified count
	  */
	def withCount(count: Int): A
	/**
	  * @param details New details to assign
	  * @return Copy of this item with the specified details
	  */
	def withDetails(details: Model): A
	/**
	  * @param errorMessages New error messages to assign
	  * @return Copy of this item with the specified error messages
	  */
	def withErrorMessages(errorMessages: Seq[String]): A
	/**
	  * @param occurrencePeriod New occurrence period to assign
	  * @return Copy of this item with the specified occurrence period
	  */
	def withOccurrencePeriod(occurrencePeriod: Span[Instant]): A
}

