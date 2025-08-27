package utopia.scribe.api.database.access.logging.error.stack

import utopia.scribe.api.database.reader.logging.StackTraceElementRecordDbReader
import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, WrapOneToManyAccess, WrapRowAccess}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessStackTraceElementRecords 
	extends AccessManyRoot[AccessStackTraceElementRecordRows[StackTraceElementRecord]] 
		with WrapRowAccess[AccessStackTraceElementRecordRows] 
		with WrapOneToManyAccess[AccessCombinedStackTraceElementRecords]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(StackTraceElementRecordDbReader)
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessStackTraceElementRecords[_, 
		_]): AccessStackTraceElementRecordValues = 
		access.values
	
	
	// IMPLEMENTED	--------------------
	
	/**
	  * @tparam A Type of accessed items
	  */
	override def apply[A](access: TargetingManyRows[A]) = AccessStackTraceElementRecordRows(access)
	
	/**
	  * @tparam A Type of accessed items
	  */
	override def apply[A](access: TargetingMany[A]) = AccessCombinedStackTraceElementRecords(access)
}

/**
  * Used for accessing multiple stack trace element records from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
  */
abstract class AccessStackTraceElementRecords[A, +Repr <: TargetingManyLike[_, Repr, 
	_]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessStackTraceElementRecord[A]] 
		with FilterStackTraceElementRecords[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible stack trace element records
	  */
	lazy val values = AccessStackTraceElementRecordValues(wrapped)
}

/**
  * Provides access to row-specific stack trace element record -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessStackTraceElementRecordRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessStackTraceElementRecords[A, AccessStackTraceElementRecordRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessStackTraceElementRecordRows[A], AccessStackTraceElementRecord[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = 
		AccessStackTraceElementRecordRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = 
		AccessStackTraceElementRecord(target)
}

/**
  * Used for accessing stack trace element record items that have been combined with one-to-many 
  * combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessCombinedStackTraceElementRecords[A](wrapped: TargetingMany[A]) 
	extends AccessStackTraceElementRecords[A, AccessCombinedStackTraceElementRecords[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedStackTraceElementRecords[A], AccessStackTraceElementRecord[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = 
		AccessCombinedStackTraceElementRecords(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = 
		AccessStackTraceElementRecord(target)
}

