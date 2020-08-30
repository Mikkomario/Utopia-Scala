package utopia.vault.nosql.factory

import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.FromModelFactory
import utopia.vault.model.immutable.{Row, Storable, Table}
import utopia.vault.sql.JoinType

import scala.util.Success

object FromRowModelFactory
{
    // OTHER    -------------------------------
    
    /**
      * Creates a new simple storable factory
      * @param table The target table
      * @return A factory for that table
      */
    def apply(table: Table): FromRowModelFactory[Storable] = TableModelFactory(table)
    
    
    // NESTED   ------------------------------
    
    private case class TableModelFactory(override val table: Table) extends FromRowModelFactory[Storable]
    {
        override def apply(model: Model[Property]) = Success(Storable(table, model))
    }
}

/**
 * These factory instances are used for converting database-originated model data into a
 * storable instance.
 * @author Mikko Hilpinen
 * @since 18.6.2017
 */
trait FromRowModelFactory[+A] extends FromRowFactory[A] with FromModelFactory[A]
{
    // IMPLEMENTED  ----------------------------
    
    override def joinType = JoinType.Inner
    
    override val joinedTables = Vector()
    
    override def apply(row: Row) = apply(row(table))
}