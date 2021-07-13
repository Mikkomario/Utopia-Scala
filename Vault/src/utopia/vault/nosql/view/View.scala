package utopia.vault.nosql.view

import utopia.vault.model.immutable.{Storable, Table}
import utopia.vault.sql.Condition

/**
  * A template trait for all access points. Doesn't specify anything about the read content but specifies the
  * settings necessary for facilitating read processes
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  */
trait View
{
	// ABSTRACT	----------------------
	
	/**
	  * @return The primary table viewed through this access point
	  */
	def table: Table
	
	/**
	  * @return Condition applied to all searches that use this access point. None if no condition should be applied.
	  */
	def globalCondition: Option[Condition]
	
	
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
