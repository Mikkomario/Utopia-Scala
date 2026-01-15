package utopia.vault.sql

import utopia.flow.operator.ordering.SelfComparable

sealed trait JoinType extends SelfComparable[JoinType]
{
    // ABSTRACT --------------------------
    
    /**
      * @return An SQL representation of this join type
      */
    def toSql: String
    
    
    // IMPLEMENTED  ----------------------
    
    override def self: JoinType = this
    
    override def toString = toSql
}

/**
 * This enumeration describes the different types of joins that can be used
 * @author Mikko Hilpinen
 * @since 30.5.2017
 */
object JoinType
{
    /**
      * All join type options
      */
    val values = Vector[JoinType](Inner, Left, Right)
    
    /**
      * Inner join only includes rows where both sides of the joins exist
      */
    object Inner extends JoinType
    {
        override val toSql = "INNER"
        
        override def compareTo(o: JoinType): Int = if (o == Inner) 0 else 1
    }
    /**
      * Left join includes all rows from the left side table and joined rows from the right side
      * table where applicable
      */
    object Left extends JoinType
    {
        override val toSql = "LEFT"
        
        override def compareTo(o: JoinType): Int = o match {
            case Left => 0
            case Right => 1
            case Inner => -1
        }
    }
    /**
      * Right join includes all rows from the right side table and joined rows from the left side
      * table where applicable
      */
    object Right extends JoinType
    {
        override val toSql = "RIGHT"
        
        override def compareTo(o: JoinType): Int = if (o == Right) 0 else -1
    }
}