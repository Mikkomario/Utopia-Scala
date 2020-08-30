package utopia.reflection.component.swing.template

import utopia.reflection.component.template.layout.stack.{Stackable, StackableWrapper}

/**
  * This wrapper wraps a stackable wrapper
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait StackableAwtComponentWrapperWrapper extends AwtComponentWrapperWrapper with StackableWrapper
{
	// ABSTRACT	---------------------
	
	override protected def wrapped: Stackable with AwtComponentRelated
}
