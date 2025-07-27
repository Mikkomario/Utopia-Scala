package utopia.scribe.core.model.factory.logging

/**
  * Common trait for error record-related factories which allow construction with individual 
  * properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait ErrorRecordFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param causeId New cause id to assign
	  * @return Copy of this item with the specified cause id
	  */
	def withCauseId(causeId: Int): A
	
	/**
	  * @param exceptionType New exception type to assign
	  * @return Copy of this item with the specified exception type
	  */
	def withExceptionType(exceptionType: String): A
	
	/**
	  * @param stackTraceId New stack trace id to assign
	  * @return Copy of this item with the specified stack trace id
	  */
	def withStackTraceId(stackTraceId: Int): A
}

