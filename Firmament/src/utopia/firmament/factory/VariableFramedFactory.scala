package utopia.firmament.factory

import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

/**
  * Common trait for (component) factories that support variable insets
  * @author Mikko Hilpinen
  * @since 09.11.2024, v1.4
  */
trait VariableFramedFactory[+A] extends FramedFactory[A]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return A pointer that contains the applied insets
	  */
	def insetsPointer: Changing[StackInsets]
	
	/**
	  * @param p New insets pointer to apply
	  * @return Copy of this factory with the specified variable insets applied
	  */
	def withInsetsPointer(p: Changing[StackInsets]): A
	
	
	// IMPLEMENTED  ---------------------
	
	override def withInsets(insets: StackInsetsConvertible): A = withInsetsPointer(Fixed(insets.toInsets))
	
	override def mapInsets(f: StackInsets => StackInsetsConvertible): A =
		withInsetsPointer(insetsPointer.map { f(_).toInsets })
		
	
	// OTHER    -------------------------
	
	/**
	  * @param f A mapping function for insets, yielding a variable result
	  * @return Copy of this factory applying the specified mapping function to insets
	  */
	def flatMapInsets(f: StackInsets => Changing[StackInsets]) = withInsetsPointer(insetsPointer.flatMap(f))
}
