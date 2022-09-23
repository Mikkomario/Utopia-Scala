package utopia.vault.model.mutable

import utopia.flow.collection.mutable.typeless.{Model, Variable}
import utopia.flow.collection.template.typeless
import utopia.flow.collection.template.typeless.Property
import utopia.flow.datastructure.mutable.Variable
import utopia.flow.datastructure.template
import utopia.flow.generic.DeclarationVariableGenerator
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
    def apply(table: Table, model: typeless.Model[Property]) =
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
class DBModel(override val table: Table) extends Model[Variable](
        new DeclarationVariableGenerator(table.toModelDeclaration)) with Storable with Readable
{
    // COMPUTED    -------------------
    
	override def valueProperties = attributes.map { v => v.name -> v.value }
	
	override def set(data: typeless.Model[Property]) = update(data)
}

/**
 * These factories are used for constructing storable models from table data
 */
class DBModelFactory(override val table: Table, override val defaultOrdering: Option[OrderBy] = None)
    extends FromRowModelFactory[DBModel]
{
    override def apply(model: typeless.Model[Property]) =
    {
        val storable = new DBModel(table)
        storable ++= model.attributes.map { p => new Variable(p.name, p.value) }
    
        Success(storable)
    }
}