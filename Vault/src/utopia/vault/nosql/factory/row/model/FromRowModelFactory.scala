package utopia.vault.nosql.factory.row.model

import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.Property
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.enumeration.SelectTarget.All
import utopia.vault.model.immutable.{Row, Storable, Table}
import utopia.vault.nosql.factory.row.FromRowFactory
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
        override def apply(model: template.ModelLike[Property]) = Success(Storable(table, model))
        override def defaultOrdering = None
    }
}

/**
 * These factory instances are used for converting database-originated model data into a Storable instance.
 * @author Mikko Hilpinen
 * @since 18.6.2017
 */
trait FromRowModelFactory[+A] extends FromRowFactory[A] with FromModelFactory[A]
{
    // IMPLEMENTED  ----------------------------
    
    override def joinedTables = Empty
    override def joinType = JoinType.Inner
    
    override def selectTarget: SelectTarget = table
    
    override def apply(row: Row) = apply(row(table))
}