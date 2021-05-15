package utopia.genesis.shape.shape2D

/**
  * Common trait for shapes that can specify a bounding box
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
trait Bounded
{
	/**
	  * @return Bounding box around this shape
	  */
	def bounds: Bounds
}
