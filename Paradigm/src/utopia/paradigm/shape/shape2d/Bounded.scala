package utopia.paradigm.shape.shape2d

/**
  * Common trait for shapes that can specify a bounding box
  * @author Mikko Hilpinen
  * @since Genesis 15.5.2021, v2.5.1
  */
// TODO: Extend sized and add a number of utility functions (or create a separate trait)
trait Bounded
{
	/**
	  * @return Bounding box around this shape
	  */
	def bounds: Bounds
}
