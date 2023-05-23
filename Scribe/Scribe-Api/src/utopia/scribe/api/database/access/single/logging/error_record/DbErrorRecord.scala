package utopia.scribe.api.database.access.single.logging.error_record

import utopia.scribe.api.database.factory.logging.ErrorRecordFactory
import utopia.scribe.api.database.model.logging.ErrorRecordModel
import utopia.scribe.core.model.stored.logging.ErrorRecord
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
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique error records.
	  * @return An access point to the error record that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueErrorRecordAccess(mergeCondition(condition))
}

