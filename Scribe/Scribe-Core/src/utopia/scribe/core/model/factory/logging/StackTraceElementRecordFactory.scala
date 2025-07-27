package utopia.scribe.core.model.factory.logging

/**
  * Common trait for stack trace element record-related factories which allow construction with 
  * individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait StackTraceElementRecordFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param causeId New cause id to assign
	  * @return Copy of this item with the specified cause id
	  */
	def withCauseId(causeId: Int): A
	
	/**
	  * @param className New class name to assign
	  * @return Copy of this item with the specified class name
	  */
	def withClassName(className: String): A
	
	/**
	  * @param fileName New file name to assign
	  * @return Copy of this item with the specified file name
	  */
	def withFileName(fileName: String): A
	
	/**
	  * @param lineNumber New line number to assign
	  * @return Copy of this item with the specified line number
	  */
	def withLineNumber(lineNumber: Int): A
	
	/**
	  * @param methodName New method name to assign
	  * @return Copy of this item with the specified method name
	  */
	def withMethodName(methodName: String): A
}

