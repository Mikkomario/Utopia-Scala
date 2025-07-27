package utopia.scribe.api.database.access.logging.issue.occurrence

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.storable.logging.IssueOccurrenceDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual issue occurrence values from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessIssueOccurrenceValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing issue occurrence database properties
	  */
	val model = IssueOccurrenceDbModel
	
	/**
	  * Access to issue occurrence id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * Id of the issue variant that occurred
	  */
	lazy val caseId = apply(model.caseId).optional { v => v.int }
	
	/**
	  * Error messages listed in the stack trace. 
	  * If multiple occurrences are represented, contains data from the latest occurrence.
	  */
	lazy val errorMessages = 
		apply(model.errorMessages) {
			 v => v.notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getVector.map { v => v.getString }; case None => Vector.empty } }
	
	/**
	  * Additional details concerning these issue occurrences.
	  * In case of multiple occurrences, contains only the latest entry for each detail.
	  */
	lazy val details = 
		apply(model.details) {
			 v => v.notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getModel; case None => Model.empty } }
	
	/**
	  * Number of issue occurrences represented by this entry
	  */
	lazy val count = apply(model.count).optional { v => v.int }
}

