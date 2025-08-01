package utopia.scribe.api.database.access.logging.error

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.scribe.api.database.access.logging.error.stack.AccessStackTraceElementRecord
import utopia.scribe.api.database.storable.logging.{ErrorRecordDbModel, StackTraceElementRecordDbModel}
import utopia.scribe.core.model.cached.logging.{RecordableError, StackTrace}
import utopia.scribe.core.model.combined.logging.ErrorRecordWithStackTrace
import utopia.scribe.core.model.partial.logging.{ErrorRecordData, StackTraceElementRecordData}
import utopia.scribe.core.model.stored.logging.{ErrorRecord, StackTraceElementRecord}
import utopia.vault.database.Connection
import utopia.vault.store.StoreResult

/**
  * Provides functions for interacting with errors in the DB
  * @author Mikko Hilpinen
  * @since 29.07.2025, v1.0.6
  */
object ErrorDb
{
	// ATTRIBUTES   ------------------
	
	private val model = ErrorRecordDbModel
	private val stackTraceModel = StackTraceElementRecordDbModel
	
	
	// OTHER    ----------------------
	
	/**
	  * Stores an error into the database. Avoids inserting duplicate entries.
	  * @param error The error to record
	  * @param connection Implicit database connection to use
	  * @return Either an error that had already been recorded (Right) or a newly inserted error record (Left)
	  */
	def store(error: RecordableError)(implicit connection: Connection): StoreResult[ErrorRecordWithStackTrace] = {
		// Stores the errors from bottom to top
		val bottomToTop = error.bottomToTop
		val errorsIterator = bottomToTop.iterator
		OptionsIterator
			.iterate(Some(_store(errorsIterator.next()))) { cause =>
				errorsIterator.nextOption().map { error => _store(error, Some(cause), cause.isNew) }
			}
			.last
	}
	private def _store(error: RecordableError, cause: Option[ErrorRecordWithStackTrace] = None,
	                  assumeNewCause: Boolean = false)
	                 (implicit connection: Connection): StoreResult[ErrorRecordWithStackTrace] =
	{
		val storedStackTrace = store(error.stackTrace)
		val data = ErrorRecordData(error.className, storedStackTrace.head.id, cause.map { _.id })
		val storedError: StoreResult[ErrorRecord] = {
			// Case: The stack trace or the cause was new => Inserts a new error (can't be duplicate)
			if (assumeNewCause || storedStackTrace.head.isNew)
				StoreResult.inserted(model.insert(data))
			// Case: The stack trace and the cause were already recorded => Checks whether this error is a duplicate
			else
				AccessErrorRecord.filter(model(data).toCondition).pull.toRight { model.insert(data) }
		}
		// Combines the collected information together
		storedError.map { error =>
			ErrorRecordWithStackTrace(error, storedStackTrace.map { _.stored }, cause)
		}
	}
	
	/**
	  * Stores a stack trace into the database.
	  * Avoids duplicate entries, where possible.
	  * @param trace      The stack trace to store
	  * @param connection Implicit DB connection
	  * @return Stored stack-trace elements. Each entry records, whether it already existed or was inserted.
	  */
	def store(trace: StackTrace)(implicit connection: Connection): Seq[StoreResult[StackTraceElementRecord]] = {
		// Stores the elements from bottom to top, avoiding duplicates
		val elements = trace.bottomToTop
		elements.oneOrMany match {
			// Case: There is only a single element => Stores it, if it is new
			case Left(single) => Single(store(stackDataFrom(single)))
			// Case: There are multiple elements to store
			case Right(elements) =>
				val remainingIterator = elements.tail.iterator
				// Checks for duplicates until a new element is inserted, after which continues to insert new elements
				// Starts by storing the bottom-most element
				OptionsIterator
					.iterate(Some(store(stackDataFrom(elements.head)))) { previous =>
						// Continues the process until there are no more elements to store
						remainingIterator.nextOption().map { next =>
							// Case: Previously stored element was new/inserted => Inserts the next element, also
							if (previous.isNew)
								StoreResult.inserted(stackTraceModel.insert(stackDataFrom(next, Some(previous.id))))
							// Case: Previously stored element was not new => Checks for duplicates on the next element too
							else
								store(stackDataFrom(next, Some(previous.id)))
						}
					}
					// Returns the elements from top-to-bottom order, so that the primary element 'trace' is the head
					.toOptimizedSeq.reverse
		}
	}
	/**
	  * Stores an individual stack trace element to the database.
	  * Avoids inserting duplicates.
	  * @param element    The element to store
	  * @param connection Implicit DB connection
	  * @return The stack track element record that existed in the DB, or that was inserted
	  */
	def store(element: StackTraceElementRecordData)
	         (implicit connection: Connection): StoreResult[StackTraceElementRecord] =
	{
		// Checks whether there already exists a matching element
		// Inserts a new entry, if necessary
		AccessStackTraceElementRecord.filter(stackTraceModel(element).toCondition).pull
			.toRight { stackTraceModel.insert(element) }
	}
	
	private def stackDataFrom(element: StackTrace, causeId: Option[Int] = None) =
		StackTraceElementRecordData(element.fileName, element.className, element.methodName, element.lineNumber, causeId)
}
