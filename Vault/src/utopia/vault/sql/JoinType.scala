package utopia.vault.sql

/**
 * This enumeration describes the different types of joins that can be used
 * @author Mikko Hilpinen
 * @since 30.5.2017
 */
object JoinType extends Enumeration
{
    type JoinType = Value
    
    /**
     * Inner join only includes rows where both sides of the joins exist
     */
    val Inner = Value("INNER")
    /**
     * Left join includes all rows from the left side table and joined rows from the right side 
     * table where applicable
     */
    val Left = Value("LEFT")
    /**
     * Right join includes all rows from the right side table and joined rows from the left side 
     * table where applicable
     */
    val Right = Value("RIGHT")
}