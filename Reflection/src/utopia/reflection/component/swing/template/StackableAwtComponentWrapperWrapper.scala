package utopia.reflection.component.swing.template

import utopia.reflection.component.template.layout.stack.{ReflectionStackable, ReflectionStackableWrapper}

/**
  * This wrapper wraps a stackable wrapper
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait StackableAwtComponentWrapperWrapper extends AwtComponentWrapperWrapper with ReflectionStackableWrapper
{
	// ABSTRACT	---------------------
	
	override protected def wrapped: ReflectionStackable with AwtComponentRelated
}
