package utopia.vault.sql

sealed trait JoinType
{
    // ABSTRACT --------------------------
    
    /**
      * @return An sql representation of this join type
      */
    def toSql: String
    
    
    // IMPLEMENTED  ----------------------
    
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
        override def toSql = "INNER"
    }
    /**
      * Left join includes all rows from the left side table and joined rows from the right side
      * table where applicable
      */
    object Left extends JoinType
    {
        override def toSql = "LEFT"
    }
    /**
      * Right join includes all rows from the right side table and joined rows from the left side
      * table where applicable
      */
    object Right extends JoinType
    {
        override def toSql = "RIGHT"
    }
}