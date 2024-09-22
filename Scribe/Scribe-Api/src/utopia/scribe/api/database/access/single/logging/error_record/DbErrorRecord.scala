package utopia.scribe.api.database.access.single.logging.error_record

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.util.EitherExtensions._
import utopia.scribe.api.database.access.single.logging.stack_trace_element_record.DbStackTraceElementRecord
import utopia.scribe.api.database.factory.logging.ErrorRecordFactory
import utopia.scribe.api.database.model.logging.ErrorRecordModel
import utopia.scribe.core.model.cached.logging.RecordableError
import utopia.scribe.core.model.combined.logging.ErrorRecordWithStackTrace
import utopia.scribe.core.model.partial.logging.ErrorRecordData
import utopia.scribe.core.model.stored.logging.{ErrorRecord, StackTraceElementRecord}
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual error records
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbErrorRecord extends SingleRowModelAccess[ErrorRecord] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ErrorRecordModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ErrorRecordFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted error record
	  * @return An access point to that error record
	  */
	def apply(id: Int) = DbSingleErrorRecord(id)
	
	/**
	  * Stores an error into the database. Avoids inserting duplicate entries.
	  * @param error The error to record
	  * @param connection Implicit database connection to use
	  * @return Either an error that had already been recorded (Right) or a newly inserted error record (Left)
	  */
	def store(error: RecordableError)(implicit connection: Connection): Sided[ErrorRecordWithStackTrace] = {
		// Stores the errors from bottom to top
		val bottomToTop = error.bottomToTop
		val errorsIterator = bottomToTop.iterator
		OptionsIterator
			.iterate(Some(store(errorsIterator.next(), Right(None)))) { cause =>
				errorsIterator.nextOption().map { error => store(error, cause.mapRight { Some(_) }) }
			}
			.last
	}
	private def store(error: RecordableError, cause: Either[ErrorRecordWithStackTrace, Option[ErrorRecordWithStackTrace]])
	                 (implicit connection: Connection): Sided[ErrorRecordWithStackTrace] =
	{
		val storedStackTrace = DbStackTraceElementRecord.store(error.stackTrace)
		val dependencies: Sided[(Seq[StackTraceElementRecord], Option[Int])] = cause match {
			case Right(existingCause) => storedStackTrace.mapEither { _ -> existingCause.map { _.id } }
			case Left(newCause) => Left(storedStackTrace.either -> Some(newCause.id))
		}
		// DependencyType is First for newly inserted values and Last for existing values
		val ((elements, appliedCauseId), dependencyType) = dependencies.eitherAndSide
		val data = ErrorRecordData(error.className, elements.head.id, appliedCauseId)
		val storedError = dependencyType match {
			// Case: The stack trace and the cause were already recorded => Checks whether this error is a duplicate
			case Last =>
				find(model(data).toCondition).toRight { model.insert(data) }
			// Case: The stack trace or the cause was new => Inserts a new error (can't be duplicate)
			case First => Left(model.insert(data))
		}
		// Combines the collected information together
		storedError.mapEither { error =>
			ErrorRecordWithStackTrace(error, elements, cause.mapLeft { Some(_) }.either)
		}
	}
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique error records.
	  * @return An access point to the error record that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueErrorRecordAccess(mergeCondition(condition))
}

