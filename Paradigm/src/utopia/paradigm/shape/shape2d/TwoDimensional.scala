package utopia.paradigm.shape.shape2d

/**
  * A common trait for dimensional items that have exactly two dimensions
  * @author Mikko Hilpinen
  * @since Genesis 21.9.2021, v2.6
  */
trait TwoDimensional[+A] extends MultiDimensional[A]
{
	override def dimensions = dimensions2D
	
	/**
	  * @return This instance's x-component
	  */
	override def x = dimensions2D.first
	/**
	  * @return This instance's y-component
	  */
	override def y = dimensions2D.second
}
