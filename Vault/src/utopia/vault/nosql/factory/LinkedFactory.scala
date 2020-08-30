package utopia.vault.nosql.factory

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.model.immutable.Row
import utopia.vault.sql.JoinType

import scala.util.{Failure, Try}

/**
 * Used for converting DB data to models for tables that contain a single link to another table / model
 * @author Mikko
 * @since 21.8.2019, v1.3.1+
 */
trait LinkedFactory[+Parent, Child] extends FromRowFactory[Parent]
{
	// ABSTRACT	---------------------
	
	/**
	 * @return Factory used for parsing linked child data
	 */
	def childFactory: FromRowFactory[Child]
	
	/**
	 * Parses model & child data into a complete parent model. Parsing failures are handled by
	 * ErrorHandling.modelParsePrinciple
	 * @param model Parent model data (not validated)
	 * @param child Parsed child
	 * @return Parsed parent data. May fail.
	 */
	def apply(model: Model[Constant], child: Child): Try[Parent]
	
	
	// IMPLEMENTED	-----------------
	
	override def joinType = JoinType.Inner
	
	override def apply(row: Row) =
	{
		if (row.containsDataForTable(childFactory.table))
			childFactory(row).flatMap { c => apply(row(table), c) }
		else
			Failure(new NoModelDataInRowException(childFactory.table, row))
	}
	
	override def joinedTables = childFactory.tables
}
