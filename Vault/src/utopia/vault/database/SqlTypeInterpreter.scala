package utopia.vault.database

import utopia.flow.generic.DataType

/**
 * These interpreters are used for converting string type descriptions (found from database data) 
 * into usable data types. The interpreters are introduced globally much like value converters, 
 * using the SqlTypeInterpreterManager object.
 * @author Mikko Hilpinen
 * @since 4.6.2017
 */
trait SqlTypeInterpreter
{
    /**
     * Interpretes an sql type string into an actual data type, if possible.
     * @param typeString The sql type string. For example: 'int(11)', 'varchar(32)' or 'timestamp'
     * @return the interpreted data type or none if the type couldn't be recognised
     */
    def apply(typeString: String): Option[DataType]
}