package utopia.vault.database

import utopia.flow.generic.model.immutable.Value

/**
 * SqlValueGenerators are able to generate value instances based on sql elements
 * @author Mikko Hilpinen
 * @since 28.4.2017
 */
trait SqlValueGenerator
{
    /**
     * Generates a new value based on an object of a specific type
     * @param value The object that should be wrapped or cast into a value. Not null.
     * @param sqlType The sql type describing the provided object
     * @see [[java.sql.Types]]
     */
    def apply(value: Any, sqlType: Int): Option[Value]
    
    /**
      * @param sqlType An integer representing an SQL data type
      * @return A function which converts a read SQL object into a [[Value]].
      *         None if conversion from the specified type is not implemented / supported.
      *
      *         Note: This function is not expected to function with null values.
      *               These should be handled separately.
      * @see [[java.sql.Types]]
      */
    def conversionFrom(sqlType: Int): Option[Any => Value]
}