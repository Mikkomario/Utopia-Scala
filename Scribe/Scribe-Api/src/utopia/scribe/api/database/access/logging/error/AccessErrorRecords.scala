package utopia.scribe.api.database.access.logging.error

import utopia.flow.collection.CollectionExtensions._
import utopia.scribe.api.database.reader.logging.ErrorRecordDbReader
import utopia.scribe.api.database.storable.logging.ErrorRecordDbModel
import utopia.scribe.core.model.stored.logging.ErrorRecord
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, WrapOneToManyAccess, WrapRowAccess}
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vault.sql.Condition

import scala.annotation.tailrec
import scala.collection.mutable

object AccessErrorRecords 
	extends AccessManyRoot[AccessErrorRecordRows[ErrorRecord]] with WrapRowAccess[AccessErrorRecordRows] 
		with WrapOneToManyAccess[AccessCombinedErrorRecords]
{
	// ATTRIBUTES	--------------------
	
	override val root = apply(ErrorRecordDbReader)
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingManyRows[A]) = AccessErrorRecordRows(access)
	override def apply[A](access: TargetingMany[A]) = AccessCombinedErrorRecords(access)
	
	
	// EXTENSIONS   --------------------
	
	implicit class ExtendedErrorRecordsAccess[R <: ErrorRecord](val a: AccessErrorRecordRows[R]) extends AnyVal
	{
		/**
		 * Collects all accessible records to a map where keys are record IDs.
		 * Includes all recorded causes.
		 * @param connection Implicit DB connection
		 * @return A map where keys are record IDs and values are error records.
		 */
		def recursiveToMapById(implicit connection: Connection): Map[Int, R] = {
			val builder = a.stream { recordsIter => mutable.Map.from(recordsIter.map { r => r.id -> r }) }
			buildRecursiveToMapById(builder,
				builder.valuesIterator.flatMap { r => r.causeId.filterNot(builder.contains) }.toIntSet)
			
			builder.toMap
		}
		@tailrec
		private def buildRecursiveToMapById(builder: mutable.Map[Int, R], nextIds: Iterable[Int])
		                                   (implicit connection: Connection): Unit =
		{
			if (nextIds.nonEmpty) {
				val idsForNextIteration = a(Condition.indexIn(ErrorRecordDbModel.id, nextIds)).stream { recordsIter =>
					recordsIter
						.flatMap { record =>
							builder += (record.id -> record)
							record.causeId.filterNot(builder.contains)
						}
						.toIntSet
				}
				buildRecursiveToMapById(builder, idsForNextIteration)
			}
		}
	}
}

/**
  * Used for accessing multiple error records from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
  */
abstract class AccessErrorRecords[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessErrorRecord[A]] with FilterErrorRecords[Repr]
		with HasValues[AccessErrorRecordValues]
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
	
	override def self = this
	
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
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedErrorRecords(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessErrorRecord(target)
}

