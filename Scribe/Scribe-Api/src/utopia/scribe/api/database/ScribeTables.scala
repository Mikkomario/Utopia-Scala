package utopia.scribe.api.database

import utopia.scribe.api.util.ScribeContext
import utopia.vault.model.immutable.Table

/**
  * Used for accessing the database tables introduced in this project
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object ScribeTables
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Table that contains error records (Represents a single error or exception thrown during 
	  * program runtime)
	  */
	lazy val errorRecord = apply("error_record")
	/**
	  * Table that contains issues (Represents a type of problem or an issue that may occur during a 
	  * program's run)
	  */
	lazy val issue = apply("issue")
	/**
	  * Table that contains issue occurrences (Represents one or more specific occurrences of a 
	  * recorded issue)
	  */
	lazy val issueOccurrence = apply("issue_occurrence")
	/**
	  * Table that contains issue variants (Represents a specific setting where a problem or an issue 
	  * occurred)
	  */
	lazy val issueVariant = apply("issue_variant")
	/**
	  * Table that contains stack trace element records (Represents a single error stack trace line.
	  * A stack trace indicates how an error propagated through the program flow before it was 
	  * recorded.)
	  */
	lazy val stackTraceElementRecord = apply("stack_trace_element_record")
	
	
	// OTHER	--------------------
	
	private def apply(tableName: String): Table = ScribeContext.table(tableName)
}

