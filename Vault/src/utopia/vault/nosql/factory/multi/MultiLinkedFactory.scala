package utopia.vault.nosql.factory.multi

import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.util.CollectionExtensions._
import utopia.vault.model.immutable.Result
import utopia.vault.nosql.factory.LinkedFactoryLike
import utopia.vault.util.ErrorHandling

import scala.util.{Failure, Success, Try}

/**
  * Used for converting DB data to model format for tables that contain a link to another table where there may be
  * multiple linked rows per 'parent' row (one to many connection).
  * @author Mikko Hilpinen
  * @since 21.8.2019, v1.3.1+
  */
trait MultiLinkedFactory[+Parent, Child] extends LinkedFactoryLike[Parent, Child]
{
	// ABSTRACT	--------------------
	
	/**
	  * Parses a parent
	  * @param id       Parent's row id
	  * @param model    Parent's model (not validated)
	  * @param children Parsed children (contains only successful results)
	  * @return Model parse results
	  */
	def apply(id: Value, model: Model[Constant], children: Vector[Child]): Try[Parent]
	
	
	// IMPLEMENTED	----------------
	
	override def apply(result: Result): Vector[Parent] =
	{
		result.grouped(table, childFactory.table).toVector.flatMap { case (id, data) =>
			val (myRow, childRows) = data
			val model = myRow(table)
			val parseResult = {
				if (childRows.nonEmpty)
					childRows.tryMap[Child, Vector[Child]] { row => childFactory(row) }
						.flatMap { children => apply(id, model, children) }
				else
					apply(id, model, Vector())
			}
			
			parseResult match {
				case Success(parent) => Some(parent)
				case Failure(error) => ErrorHandling.modelParsePrinciple.handle(error); None
			}
		}
	}
}
