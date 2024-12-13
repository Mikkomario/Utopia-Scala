package utopia.firmament.context

/**
  * Common trait for context instances that may appear in a static or a variable form,
  * and which can be converted between the two.
  * @tparam Static A static variant of this context
  * @tparam Variable A variable variant of this context
  * @author Mikko Hilpinen
  * @since 12.12.2024, v1.4
  */
trait DualFormContext[+Static, +Variable]
{
	/**
	  * @return Current state of this context as a static context instance
	  */
	def current: Static
	/**
	  * @return This context as a variable context instance
	  */
	def toVariableContext: Variable
}
