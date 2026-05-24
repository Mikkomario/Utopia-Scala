package utopia.paradigm.shape.template

/**
  * A common trait for factories that can wrap dimensions into other items
  * @author Mikko Hilpinen
  * @since 7.11.2022, v1.2
  */
trait FromDimensionsFactory[-D, +To]
{
	/**
	  * @param dimensions A set of dimensions
	  * @return An item wrapping those dimensions
	  */
	def apply(dimensions: Dimensions[D]): To
	
	/**
	  * @param other An item with dimensions
	  * @return An item of the correct type from that item or its dimensions
	  */
	def from(other: HasDimensions[D]): To
}
