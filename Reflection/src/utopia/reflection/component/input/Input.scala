package utopia.reflection.component.input

/**
  * Inputs can be used for reading user input
  * @author Mikko Hilpinen
  * @since 22.4.2019, v1+
  */
trait Input[+A]
{
	/**
	  * @return The current input in this input
	  */
	def value: A
}
