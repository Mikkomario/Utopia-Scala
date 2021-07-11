package utopia.vault.nosql.access.template

import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Storable, Table}
import utopia.vault.sql.{Condition, OrderBy}

/**
  * A common trait for all DB access points
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  * @tparam A The type of search results this access point produces
  */
trait Access[+A]
{
	// ABSTRACT	----------------------
	
	/**
	  * @return The primary table accessed through this access point
	  */
	def table: Table
	
	/**
	  * @return Condition applied to all searches that use this access point. None if no condition should be applied.
	  */
	def globalCondition: Option[Condition]
	
	/**
	  * Performs the actual data read + possible wrapping
	  * @param condition  Final search condition used when reading data (None if no condition should be applied)
	  * @param order      The ordering applied to the data read (None if no ordering)
	  * @param connection Database connection used (implicit)
	  * @return Read data
	  */
	protected def read(condition: Option[Condition], order: Option[OrderBy] = None)(implicit connection: Connection): A
	
	
	// OTHER	----------------------
	
	/**
	  * Merges an additional condition with the existing global condition
	  * @param additional An additional condition
	  * @return A combination of the additional and global conditions
	  */
	def mergeCondition(additional: Condition) = globalCondition.map { _ && additional }.getOrElse(additional)
	
	/**
	  * Merges an additional condition with the existing global condition
	  * @param additional An additional condition (optional)
	  * @return A combination of the additional and global conditions. None if there was neither.
	  */
	def mergeCondition(additional: Option[Condition]): Option[Condition] = additional match {
		case Some(cond) => Some(mergeCondition(cond))
		case None => globalCondition
	}
	
	/**
	  * Merges an additional condition with the existing global condition
	  * @param conditionModel A model representing the additional condition to apply
	  * @return A combination of these conditions
	  */
	def mergeCondition(conditionModel: Storable): Condition = mergeCondition(conditionModel.toCondition)
}
