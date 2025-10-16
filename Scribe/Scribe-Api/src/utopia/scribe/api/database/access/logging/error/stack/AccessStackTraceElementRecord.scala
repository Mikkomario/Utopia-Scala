package utopia.scribe.api.database.access.logging.error.stack

import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

object AccessStackTraceElementRecord 
	extends AccessOneRoot[AccessStackTraceElementRecord[StackTraceElementRecord]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessStackTraceElementRecords.root.head
}

/**
  * Used for accessing individual stack trace element records from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
  */
case class AccessStackTraceElementRecord[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessStackTraceElementRecord[A]] 
		with FilterStackTraceElementRecords[AccessStackTraceElementRecord[A]]
		with HasValues[AccessStackTraceElementRecordValue]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible stack trace element record
	  */
	lazy val values = AccessStackTraceElementRecordValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessStackTraceElementRecord(newTarget)
}

