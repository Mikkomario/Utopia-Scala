package utopia.firmament.factory

import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}

/**
  * Common trait for (component) factories that apply static insets
  * @author Mikko Hilpinen
  * @since 09.11.2024, v1.4
  */
trait StaticFramedFactory[+A] extends FramedFactory[A]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return The currently specified insets
	  */
	def insets: StackInsets
	
	
	// IMPLEMENTED  -------------------------
	
	override def mapInsets(f: StackInsets => StackInsetsConvertible): A = withInsets(f(insets))
}
