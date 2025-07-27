package utopia.scribe.api.database.access.logging.error

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.logging.ErrorRecordDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on error record properties
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
trait FilterErrorRecords[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines error record database properties
	  */
	def model = ErrorRecordDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param causeId cause id to target
	  * @return Copy of this access point that only includes error records with the specified cause id
	  */
	def causedBy(causeId: Int) = filter(model.causeId.column <=> causeId)
	
	/**
	  * @param causeIds Targeted cause ids
	  * @return Copy of this access point that only includes error records where cause id is within the 
	  * specified value set
	  */
	def causedByErrors(causeIds: IterableOnce[Int]) = filter(model.causeId.column.in(IntSet.from(causeIds)))
	
	/**
	  * @param exceptionType exception type to target
	  * @return Copy of this access point that only includes error records with the specified exception type
	  */
	def ofType(exceptionType: String) = filter(model.exceptionType.column <=> exceptionType)
	
	/**
	  * @param exceptionTypes Targeted exception types
	  * @return Copy of this access point that only includes error records where exception type is within the 
	  * specified value set
	  */
	def ofTypes(exceptionTypes: Iterable[String]) = filter(model.exceptionType.column.in(exceptionTypes))
	
	/**
	  * @param stackTraceId stack trace id to target
	  * @return Copy of this access point that only includes error records with the specified stack trace id
	  */
	def withStack(stackTraceId: Int) = filter(model.stackTraceId.column <=> stackTraceId)
	
	/**
	  * @param stackTraceIds Targeted stack trace ids
	  * @return Copy of this access point that only includes error records where stack trace id is within the 
	  * specified value set
	  */
	def withStacks(stackTraceIds: IterableOnce[Int]) = 
		filter(model.stackTraceId.column.in(IntSet.from(stackTraceIds)))
}

