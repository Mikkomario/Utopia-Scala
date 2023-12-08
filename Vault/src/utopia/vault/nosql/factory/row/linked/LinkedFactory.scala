package utopia.vault.nosql.factory.row.linked

import utopia.flow.generic.model.immutable.Model
import utopia.vault.model.error.NoModelDataInRowException
import utopia.vault.model.immutable.Row
import utopia.vault.nosql.factory.LinkedFactoryLike
import utopia.vault.nosql.factory.row.FromRowFactory

import scala.util.{Failure, Try}

/**
  * Used for converting DB data to models for tables that contain a single link to another table / model.
  * Expects there to always exist a link between the two tables
  * @author Mikko Hilpinen
  * @since 21.8.2019, v1.3.1
  */
trait LinkedFactory[+Parent, Child] extends FromRowFactory[Parent] with LinkedFactoryLike[Parent, Child]
{
	// ABSTRACT	---------------------
	
	/**
	  * Parses model & child data into a complete parent model. Parsing failures are handled by
	  * ErrorHandling.modelParsePrinciple
	  * @param model Parent model data (not validated)
	  * @param child Parsed child
	  * @return Parsed parent data. May fail.
	  */
	def apply(model: Model, child: Child): Try[Parent]
	
	
	// IMPLEMENTED	-----------------
	
	override def isAlwaysLinked = true
	
	override def apply(row: Row) = {
		if (row.containsDataForTable(childFactory.table))
			childFactory(row).flatMap { c => apply(row(table), c) }
		else
			Failure(new NoModelDataInRowException(childFactory.table, row))
	}
}
