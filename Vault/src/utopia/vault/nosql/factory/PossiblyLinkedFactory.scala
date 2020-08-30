package utopia.vault.nosql.factory
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.model.immutable.Row
import utopia.vault.sql.JoinType

import scala.util.Try

/**
  * Used for reading model data when the read element is linked to 0-1 other items in another table
  * @author Mikko Hilipnen
  * @since 16.5.2020, v1.6
  */
trait PossiblyLinkedFactory[+Parent, Child] extends FromRowFactory[Parent]
{
	// ABSTRACT	------------------------------
	
	/**
	  * @return Factory used for reading possible child data
	  */
	def childFactory: FromRowFactory[Child]
	
	/**
	  * Parses a model from read data
	  * @param model Model read from the row
	  * @param child Child data that was parsed. None if no child data was present or none could be parsed.
	  * @return Read data. Failure if parsing failed.
	  */
	def apply(model: Model[Constant], child: Option[Child]): Try[Parent]
	
	
	// IMPLEMENTED	--------------------------
	
	override def apply(row: Row): Try[Parent] =
	{
		val child = childFactory.parseIfPresent(row)
		apply(row(table), child)
	}
	
	override def joinedTables = childFactory.tables
	
	override def joinType = JoinType.Left
}
