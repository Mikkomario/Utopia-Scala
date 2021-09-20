package utopia.flow.operator

/**
  * A common trait for items which can be measured as a linear length
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait LinearMeasurable extends Any
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Linear length of this item
	  */
	def length: Double
}
