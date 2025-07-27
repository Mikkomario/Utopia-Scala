package utopia.scribe.api.database.access.logging.error

import utopia.scribe.core.model.stored.logging.ErrorRecord
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessErrorRecord extends AccessOneRoot[AccessErrorRecord[ErrorRecord]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessErrorRecords.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessErrorRecord[_]): AccessErrorRecordValue = access.values
}

/**
  * Used for accessing individual error records from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessErrorRecord[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessErrorRecord[A]] with FilterErrorRecords[AccessErrorRecord[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible error record
	  */
	lazy val values = AccessErrorRecordValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessErrorRecord(newTarget)
}

