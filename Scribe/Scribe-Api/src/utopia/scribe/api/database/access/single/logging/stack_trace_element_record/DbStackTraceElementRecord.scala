package utopia.scribe.api.database.access.single.logging.stack_trace_element_record

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.scribe.api.database.factory.logging.StackTraceElementRecordFactory
import utopia.scribe.api.database.model.logging.StackTraceElementRecordModel
import utopia.scribe.core.model.cached.logging.StackTrace
import utopia.scribe.core.model.partial.logging.StackTraceElementRecordData
import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

import scala.collection.immutable.VectorBuilder

/**
  * Used for accessing individual stack trace elements
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbStackTraceElementRecord extends SingleRowModelAccess[StackTraceElementRecord] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = StackTraceElementRecordModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StackTraceElementRecordFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted stack trace element
	  * @return An access point to that stack trace element
	  */
	def apply(id: Int) = DbSingleStackTraceElementRecord(id)
	
	/**
	  * Stores a stack trace into the database.
	  * Avoids duplicate entries, where possible.
	  * @param trace The stack trace to store
	  * @param connection Implicit DB connection
	  * @return Either
	  *             Left) Stored stack trace elements, where at least one of them was new,
	  *             Right) Stack trace elements that already existed in the database.
	  *                    No new inserts have been made.
	  */
	def store(trace: StackTrace)(implicit connection: Connection): Either[Vector[StackTraceElementRecord], Vector[StackTraceElementRecord]] = {
		// Stores the elements from bottom to top, avoiding duplicates
		val elements = trace.bottomToTop
		elements.oneOrMany match {
			// Case: There is only a single element => Stores it, if it is new
			case Left(single) => store(dataFrom(single)).mapEither { Vector(_) }
			// Case: There are multiple elements to store
			case Right(elements) =>
				val remainingIterator = elements.tail.iterator
				// Checks for duplicates until a new element is inserted, after which continues to insert new elements
				// Starts by storing the bottom-most element
				val storedElements = OptionsIterator.iterate(Some(store(dataFrom(elements.head)))) { previous =>
					// Continues the process until there are no more elements to store
					remainingIterator.nextOption().map { next =>
						previous match {
							// Case: Previously stored element was not new
							// => Checks for duplicates on the next element too
							case Right(existingPrevious) => store(dataFrom(next, Some(existingPrevious.id)))
							// Case: Previously stored element was new/inserted => Inserts the next element, also
							case Left(newPrevious) => Left(model.insert(dataFrom(next, Some(newPrevious.id))))
						}
					}
				}.toVector
				// Returns Right if all elements existed already and Left otherwise
				storedElements.last.mapEither { _ => storedElements.map { _.either } }
		}
	}
	/**
	  * Stores an individual stack trace element to the database.
	  * Avoids inserting duplicates.
	  * @param element The element to store
	  * @param connection Implicit DB connection
	  * @return Either
	  *             Left) Newly inserted stack trace element, or
	  *             Right) A matching element that already existed in the database, in which case no insert is made
	  */
	def store(element: StackTraceElementRecordData)(implicit connection: Connection) = {
		// Checks whether there already exists a matching element
		// Inserts a new entry, if necessary
		find(model(element).toCondition).toRight { model.insert(element) }
	}
	private def dataFrom(element: StackTrace, causeId: Option[Int] = None) =
		StackTraceElementRecordData(element.className, element.methodName, element.lineNumber, causeId)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique stack trace elements.
	  * @return An access point to the stack trace element that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueStackTraceElementRecordAccess(mergeCondition(condition))
}

