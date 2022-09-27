package utopia.vault.model.mutable

import utopia.flow.generic.model.mutable.{MutableModel, Variable}
import utopia.flow.generic.model.template.Property
import utopia.flow.generic.model.template
import utopia.vault.model.immutable.{Storable, Table}
import utopia.vault.nosql.factory.row.model.FromRowModelFactory
import utopia.vault.sql.OrderBy

import scala.util.Success

object DBModel
{
    /**
     * Creates a new factory for storable models of a certain table
     */
    def makeFactory(table: Table) = new DBModelFactory(table)
    
    /**
     * Wraps a model into a db model
     */
    def apply(table: Table, model: template.ModelLike[Property]) =
    {
        val result = new DBModel(table)
        result.set(model)
        result
    }
}

/**
* These mutable models can be used as simple storable instances
* @author Mikko Hilpinen
* @since 22.5.2018
**/
class DBModel(override val table: Table)
    extends MutableModel[Variable](Vector(), table.toModelDeclaration.toVariableFactory) with Storable with Readable
{
    // COMPUTED    -------------------
    
	override def valueProperties = properties.map { v => v.name -> v.value }
	
	override def set(data: template.ModelLike[Property]) =
        data.properties.foreach { p => update(p.name, p.value) }
}

/**
 * These factories are used for constructing storable models from table data
 */
class DBModelFactory(override val table: Table, override val defaultOrdering: Option[OrderBy] = None)
    extends FromRowModelFactory[DBModel]
{
    override def apply(model: template.ModelLike[Property]) =
    {
        val storable = new DBModel(table)
        storable ++= model.properties.map { p => Variable(p.name, p.value) }
    
        Success(storable)
    }
}