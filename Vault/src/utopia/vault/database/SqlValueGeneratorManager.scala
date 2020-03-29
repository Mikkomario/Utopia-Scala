package utopia.vault.database

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.AnyType

/**
 * This class uses multiple sql value generators and generates values that way
 * @author Mikko Hilpinen
 * @since 7.5.2017
 */
class SqlValueGeneratorManager(initialGenerators: Iterable[SqlValueGenerator])
{
    // ATTRIBUTES    --------------------
    
    private var generators = Vector[SqlValueGenerator]() ++ initialGenerators
    
    
    // OPERATORS    ---------------------
    
    /**
     * Generates a new value based on sql data.
     * @return A value generated based on the sql data. Returns an empty value in case one couldn't
     * be generated or if the provided object was null
     */
    def apply(value: Any, sqlType: Int) = 
    {
        if (value == null)
        {
            new Value(None, AnyType)
        }
        else 
        {
            generators.view.map { _(value, sqlType) }.find {
                _.isDefined }.flatten.getOrElse(new Value(None, AnyType))
        }
    }
    
    
    // OTHER METHODS    -----------------
    
    /**
     * Adds a new value generator to the set of available generators. The new generator will take
     * priority over existing ones
     */
    def introduce(generator: SqlValueGenerator) = generators +:= generator
}