package utopia.vault.model.mutable

import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.generic.model.immutable.Value
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.vault.model.immutable.{Result, Row}

import scala.util.{Failure, Success, Try}

object ResultStream
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * An empty result stream
	  */
	lazy val empty = new ResultStream()
}

/**
  * Represents a result acquired for a database query.
  * The resulting database rows are acquired lazily / streamed, and not cached by default.
  * @constructor Creates a new result stream
  * @param closedFlag A flag that is set once the underlying stream is closed.
  *                   When set, [[rowsIterator]] and [[generatedKeysIterator]] won't yield any results,
  *                   and [[lazyUpdatedRowsCount]] may fail if called.
  * @param failurePointer A pointer that catches the first encountered failure.
  *                       Contains None while no failures have been encountered.
  * @param rowsIterator An iterator that (lazily) yields all accessed database rows
  * @param generatedKeysIterator An iterator that yields all auto-generated keys (applicable to insert statements)
  * @param lazyUpdatedRowsCount A lazily initialized container, which will contain the number of updated rows
  *                             (applicable for update statements).
  *                             NB: Initializing this value will cause [[rowsIterator]] to be flushed / discarded.
  * @author Mikko Hilpinen
  * @since 30.06.2025, v1.22
  */
class ResultStream(val closedFlag: Flag = AlwaysTrue,
                   val failurePointer: Changing[Option[Throwable]] = Fixed.never,
                   val rowsIterator: Iterator[Row] = Iterator.empty,
                   val generatedKeysIterator: Iterator[Value] = Iterator.empty,
                   val lazyUpdatedRowsCount: Lazy[Try[Int]] = Lazy.initialized(Success(0)))
{
	// COMPUTED --------------------------
	
	/**
	  * Buffers the remaining results.
	  * Throws if any errors are or were encountered
	  * @return A buffered result containing the remaining of the available rows.
	  *         Failure if a failure was encountered before or during parsing.
	  */
	def buffer = tryBuffer.get
	/**
	  * @return A buffered result containing the remaining of the available rows.
	  *         Failure if a failure was encountered before or during parsing.
	  */
	def tryBuffer = failurePointer.value match {
		// Case: Already failed => Yields a failure
		case Some(error) => Failure(error)
		case None =>
			// Reads the rows and the generated keys
			val rows = OptimizedIndexedSeq.from(rowsIterator)
			val keys = OptimizedIndexedSeq.from(generatedKeysIterator)
			// Acquires the update count, if possible
			tryUpdatedRowsCount.flatMap { updateCount =>
				// Checks whether failed during this process
				failurePointer.value match {
					// Case: Failed => Yields a failure
					case Some(error) => Failure(error)
					// Case: Didn't fail => Succeeds
					case None => Success(Result(rows, keys, updateCount))
				}
			}
	}
	
	/**
	  * Acquires the number of updated rows.
	  * NB: Causes all other content to be consumed / discarded.
	  * Throws if there were any errors during the query.
	  * @return Number of updated rows
	  */
	def updatedRowsCount = tryUpdatedRowsCount.get
	/**
	  * Acquires the number of updated rows.
	  * NB: Causes all other content to be consumed / discarded.
	  * @return Number of updated rows.
	  *         Failure if there was an error during statement execution or results-processing.
	  */
	def tryUpdatedRowsCount = lazyUpdatedRowsCount.value
	/**
	  * Checks whether any rows were updated
	  * NB: Causes all other content to be consumed / discarded.
	  * @return Whether the query updated any rows
	  */
	def updatedRows = tryUpdatedRowsCount.toOption.exists { _ > 0 }
	
	/**
	  * @return An iterator that yields all remaining rows as models.
	  *         Should only be used with queries involving individual tables.
	  */
	def rowModelsIterator = rowsIterator.map { _.toModel }
	/**
	  * @return An iterator that yields all remaining rows as values.
	  *         Should only be used for queries targeting individual columns.
	  */
	def rowValuesIterator = rowsIterator.map { _.value }
	/**
	  * @return An iterator that yields all remaining rows as integer values. Empty / NULL values are excluded.
	  *         Should only be used for queries targeting individual columns.
	  */
	def rowIntValuesIterator = rowValuesIterator.flatMap { _.int }
	
	/**
	  * @return A model representing the next available row.
	  *         None if no more rows are available.
	  */
	def nextModel = rowsIterator.nextOption().map { _.toModel }
	/**
	  * @return A value read from the next available row.
	  *         Should only be used with queries targeting individual columns.
	  */
	def nextValue = rowsIterator.nextOption() match {
		case Some(row) => row.value
		case None => Value.empty
	}
	
	/**
	  * An iterator that yields the auto-generated keys as integers
	  */
	def generatedIntKeysIterator = generatedKeysIterator.flatMap { _.int }
	/**
	  * An iterator that yields the auto-generated keys as long numbers
	  */
	def generatedLongKeysIterator = generatedKeysIterator.flatMap { _.long }
}
