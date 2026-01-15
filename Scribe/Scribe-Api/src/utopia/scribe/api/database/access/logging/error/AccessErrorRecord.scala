package utopia.scribe.api.database.access.logging.error

import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.scribe.core.model.stored.logging.ErrorRecord
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessErrorRecord extends AccessOneRoot[AccessErrorRecord[ErrorRecord]]
{
	// ATTRIBUTES	--------------------
	
	override val root = AccessErrorRecords.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessErrorRecord[_]): AccessErrorRecordValue = access.values
	
	
	// EXTENSIONS   -----------------
	
	implicit class RichAccessErrorRecord(val a: AccessErrorRecord[ErrorRecord]) extends AnyVal
	{
		// COMPUTED ------------------------
		
		/**
		  * An iterator that returns this error and all the underlying errors
		  * @param c Implicit DB connection - Should be kept open during the whole iteration
		  */
		def topToBottomIterator(implicit c: Connection) =
			OptionsIterator.iterate(a.pull) { error => error.causeId.flatMap { AccessErrorRecord(_).pull } }
	}
}

/**
  * Used for accessing individual error records from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
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
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessErrorRecord(newTarget)
}

