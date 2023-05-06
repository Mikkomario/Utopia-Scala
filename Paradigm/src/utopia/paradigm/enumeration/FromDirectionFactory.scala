package utopia.paradigm.enumeration

/**
  * Common trait for classes that can build items based on a direction
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.3.1
  */
trait FromDirectionFactory[+A]
{
	// ABSTRACT -------------------------
	
	/**
	  * @param direction A direction
	  * @return An item based on that direction
	  */
	def apply(direction: Direction2D): A
	
	
	// COMPUTED ------------------------
	
	def left = apply(Direction2D.Left)
	def right = apply(Direction2D.Right)
	def up = apply(Direction2D.Up)
	def down = apply(Direction2D.Down)
}
