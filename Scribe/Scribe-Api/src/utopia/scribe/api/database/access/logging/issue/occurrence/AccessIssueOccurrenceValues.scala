package utopia.scribe.api.database.access.logging.issue.occurrence

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.storable.logging.IssueOccurrenceDbModel
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing issue occurrence values from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessIssueOccurrenceValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing issue occurrence database properties
	  */
	val model = IssueOccurrenceDbModel
	
	/**
	  * Access to issue occurrence ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	/**
	  * Id of the issue variant that occurred
	  */
	lazy val caseIds = apply(model.caseId) { v => v.getInt }
	/**
	  * Error messages listed in the stack trace. 
	  * If multiple occurrences are represented, contains data from the latest occurrence.
	  */
	lazy val errorMessages = apply(model.errorMessages) { v =>
		v.notEmpty match {
			case Some(v) => JsonBunny.sureMunch(v.getString).getVector.map { v => v.getString }
			case None => Empty
		}
	}
	/**
	  * Additional details concerning these issue occurrences.
	  * In case of multiple occurrences, contains only the latest entry for each detail.
	  */
	lazy val details = apply(model.details) { v =>
		v.notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getModel; case None => Model.empty }
	}
	/**
	  * Number of issue occurrences represented by this entry
	  */
	lazy val counts = apply(model.count) { v => v.getInt }
	
	
	// COMPUTED -------------------------
	
	/**
	  * @param connection Implicit DB connection
	  * @return Total number of accessible issue occurrences
	  */
	def totalCount(implicit connection: Connection) = access.streamColumn(model.count) { _.map { _.getInt }.sum }
}

