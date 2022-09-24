package utopia.flow.generic.model.immutable

import utopia.flow.generic.model.enumeration.ConversionReliability
import utopia.flow.generic.model.mutable.DataType

/**
  * A conversion contains information about a conversion between two data types, including the
  * reliability of the conversion
  * @author Mikko Hilpinen
  * @since 7.11.2016
  * @param source      The source data type
  * @param target      The target data type
  * @param reliability The reliability of the conversion
  */
case class Conversion(source: DataType, target: DataType, reliability: ConversionReliability)
{
	// COMP. PROPERTIES    -------
	
	/**
	  * The cost of this conversion in an arbitrary relative unit
	  */
	def cost = reliability.cost
	
	
	// IMPLEMENTED METHODS    ----
	
	override def toString = s"conversion from $source to $target ($reliability)"
}
