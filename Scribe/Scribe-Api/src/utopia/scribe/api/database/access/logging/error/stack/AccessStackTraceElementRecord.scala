package utopia.scribe.api.database.access.logging.error.stack

import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.view.template.Extender
import utopia.scribe.core.model.partial.logging.StackTraceElementRecordData
import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

object AccessStackTraceElementRecord 
	extends AccessOneRoot[AccessStackTraceElementRecord[StackTraceElementRecord]]
{
	// ATTRIBUTES	--------------------
	
	override val root = AccessStackTraceElementRecords.root.head
	
	
	// EXTENSIONS   --------------------
	
	implicit class RichAccessStackTraceElementRecord[A <: Extender[StackTraceElementRecordData]]
	(val a: AccessStackTraceElementRecord[A]) extends AnyVal
	{
		/**
		 * @param connection Implicit DB connection - Should be kept open throughout the full iteration
		 * @return An iterator that yields this item,
		 *         followed by the causative records from direct cause to the root cause.
		 */
		def topToBottomIterator(implicit connection: Connection) = {
			val index = a.index
			OptionsIterator.iterate(a.pull) { _.causeId.flatMap { id => a(index <=> id).pull } }
		}
	}
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
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessStackTraceElementRecord(newTarget)
}

