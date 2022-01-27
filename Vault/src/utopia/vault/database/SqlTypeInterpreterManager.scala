package utopia.vault.database

/**
 * This global object is used for converting sql type strings into data types. This manager uses 
 * various sql type interpreters that have been introduced to it. All but the basic implementation 
 * must be introduced separately.
 * @author Mikko Hilpinen
 * @since 4.6.2017
 */
object SqlTypeInterpreterManager
{
    // ATTRIBUTES    -------------------
    
    private var interpreters = Vector[SqlTypeInterpreter](BasicSqlTypeInterpreter)
    
    
    // OPERATORS    --------------------
    
    /**
     * Interprets a type string into a data type using the introduced interpreters.
      * Also returns found column length limitation.
     */
    def apply(typeString: String) =
        interpreters.reverseIterator.map { _(typeString) }.find { _._1.isDefined }.getOrElse { None -> None }
    
    
    // OTHER METHODS    ----------------
    
    /**
     * Introduces a new interpreter to the available options. The last added interpreters will 
     * be used first and when they cannot interpret a type, an earlier interpreter is used
     */
    def introduce(interpreter: SqlTypeInterpreter) = interpreters = interpreters :+ interpreter
}