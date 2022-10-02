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
     * @see java.sql.Types
     */
    def apply(value: Any, sqlType: Int): Option[Value]
}