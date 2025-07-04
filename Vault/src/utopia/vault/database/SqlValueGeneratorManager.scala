package utopia.vault.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.generic.model.immutable.Value

/**
  * This class uses multiple sql value generators and generates values that way
  * @author Mikko Hilpinen
  * @since 7.5.2017
  */
class SqlValueGeneratorManager(initialGenerators: Iterable[SqlValueGenerator])
{
	// ATTRIBUTES    --------------------
	
	private var generators = OptimizedIndexedSeq.from(initialGenerators)
	
	
	// OTHER METHODS    -----------------
	
	/**
	  * Generates a new value based on sql data.
	  * @return A value generated based on the sql data. Returns an empty value in case one couldn't
	  * be generated or if the provided object was null
	  */
	def apply(value: Any, sqlType: Int) = {
		if (value == null)
			Value.empty
		else
			generators.findMap { _(value, sqlType) }.getOrElse(Value.empty)
	}
	/**
	  * @param sqlType SQL type to convert
	  * @return A function for converting SQL values of that type to [[Value]]s.
	  *         None if this conversion is not supported.
	  */
	// Finds a conversion that's possible from the specified SQL data type
	def conversionFrom(sqlType: Int): Option[Any => Value] =
		generators.findMap { _.conversionFrom(sqlType) }
			// Includes handling of null values separately
			.map { conversion => v: Any => if (v == null) Value.empty else conversion(v) }
	
	/**
	  * Adds a new value generator to the set of available generators. The new generator will take
	  * priority over existing ones
	  */
	def introduce(generator: SqlValueGenerator) = generators +:= generator
}