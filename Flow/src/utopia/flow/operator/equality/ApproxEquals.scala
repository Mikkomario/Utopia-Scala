package utopia.flow.operator.equality

/**
  * Classes extending this trait can be compared with instances of a class using approximate comparison
  * @author Mikko Hilpinen
  * @since 1.8.2017
  */
trait ApproxEquals[-A] extends Any
{
	// ABSTRACT ---------------------
	
	/**
	  * Checks whether the two instances are approximately equal
	  */
	def ~==(other: A): Boolean
	
	
	// OTHER    ---------------------
	
	/**
	  * Checks whether the two instances are <b>not</b> approximately equal
	  */
	def !~==(other: A) = !(this ~== other)
}
