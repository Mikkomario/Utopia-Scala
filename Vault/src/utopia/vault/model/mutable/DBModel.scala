package utopia.vault.model.mutable

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.datastructure.mutable.{Model, Variable}
import utopia.flow.datastructure.{immutable, template}
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.DeclarationVariableGenerator
import utopia.vault.nosql.factory.FromRowModelFactory
import utopia.vault.model.immutable.{Storable, Table}

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
    def apply(table: Table, model: template.Model[Property]) = 
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
	
	override def set(data: template.Model[Property]) = update(data)
}

/**
 * These factories are used for constructing storable models from table data
 */
class DBModelFactory(val table: Table) extends FromRowModelFactory[DBModel]
{
    override def apply(model: template.Model[Property]) =
    {
        val storable = new DBModel(table)
        storable ++= model.attributes.map { p => new Variable(p.name, p.value) }
    
        Success(storable)
    }
}