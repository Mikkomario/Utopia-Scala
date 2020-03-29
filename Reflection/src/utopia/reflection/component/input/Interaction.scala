package utopia.reflection.component.input

/**
  * Interactions are Inputs that can also be adjusted from program side. Kind of like a mutable version of input.
  * @author Mikko Hilpinen
  * @since 22.4.2019, v1+
  */
trait Interaction[A] extends Input[A]
{
	/**
	  * Updates the value of this interaction element
	  * @param newValue New (input) value for this interaction
	  */
	def value_=(newValue: A): Unit
}
