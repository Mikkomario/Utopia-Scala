package utopia.reflection.component.swing.template

import utopia.reflection.component.template.{ReflectionComponentLike, ReflectionComponentWrapper}

/**
  * This wrapper wraps another wrapper
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait AwtComponentWrapperWrapper extends ReflectionComponentWrapper with AwtComponentRelated
{
	// ABSTRACT	----------------------
	
	protected def wrapped: ReflectionComponentLike with AwtComponentRelated
	
	
	// IMPLEMENTED	------------------
	
	override def component = wrapped.component
}
