package utopia.firmament.model.stack

/**
  * A common trait for shapes that can be used as stack insets
  * @author Mikko Hilpinen
  * @since 11.10.2020, Reflection v2
  */
trait StackInsetsConvertible
{
	// ABSTRACT	------------------------------
	
	/**
	  * @return Insets based on this shape
	  */
	def toInsets: StackInsets
}
