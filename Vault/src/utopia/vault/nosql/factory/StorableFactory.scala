package utopia.vault.nosql.factory

import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.FromModelFactory
import utopia.vault.model.immutable.{Row, Storable, Table}

import scala.util.Success

object StorableFactory
{
    /**
      * Creates a new simple storable factory
      * @param table The target table
      * @return A factory for that table
      */
    def apply(table: Table): StorableFactory[Storable] = new ImmutableStorableFactory(table)
}

/**
 * These factory instances are used for converting database-originated model data into a 
 * storable instance.
 * @author Mikko Hilpinen
 * @since 18.6.2017
 */
trait StorableFactory[+A] extends FromRowFactory[A] with FromModelFactory[A]
{
    // IMPLEMENTED  ----------------------------
    
    override val joinedTables = Vector()
    
    // Handles parsing errors
    override def apply(row: Row) = apply(row(table))
}

private class ImmutableStorableFactory(override val table: Table) extends StorableFactory[Storable]
{
    override def apply(model: Model[Property]) = Success(Storable(table, model))
}