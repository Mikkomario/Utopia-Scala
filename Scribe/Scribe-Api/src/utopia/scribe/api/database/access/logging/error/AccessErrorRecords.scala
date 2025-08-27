package utopia.scribe.api.database.access.logging.error

import utopia.scribe.api.database.reader.logging.ErrorRecordDbReader
import utopia.scribe.core.model.stored.logging.ErrorRecord
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, WrapOneToManyAccess, WrapRowAccess}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessErrorRecords 
	extends AccessManyRoot[AccessErrorRecordRows[ErrorRecord]] with WrapRowAccess[AccessErrorRecordRows] 
		with WrapOneToManyAccess[AccessCombinedErrorRecords]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(ErrorRecordDbReader)
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessErrorRecords[_, _]): AccessErrorRecordValues = access.values
	
	
	// IMPLEMENTED	--------------------
	
	/**
	  * @tparam A Type of accessed items
	  */
	override def apply[A](access: TargetingManyRows[A]) = AccessErrorRecordRows(access)
	
	/**
	  * @tparam A Type of accessed items
	  */
	override def apply[A](access: TargetingMany[A]) = AccessCombinedErrorRecords(access)
}

/**
  * Used for accessing multiple error records from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
  */
abstract class AccessErrorRecords[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessErrorRecord[A]] with FilterErrorRecords[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible error records
	  */
	lazy val values = AccessErrorRecordValues(wrapped)
}

/**
  * Provides access to row-specific error record -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessErrorRecordRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessErrorRecords[A, AccessErrorRecordRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessErrorRecordRows[A], AccessErrorRecord[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessErrorRecordRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessErrorRecord(target)
}

/**
  * Used for accessing error record items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessCombinedErrorRecords[A](wrapped: TargetingMany[A]) 
	extends AccessErrorRecords[A, AccessCombinedErrorRecords[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedErrorRecords[A], AccessErrorRecord[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedErrorRecords(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessErrorRecord(target)
}

