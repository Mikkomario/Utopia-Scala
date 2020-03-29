package utopia.reflection.component.swing

import utopia.reflection.component.{ComponentLike, ComponentWrapper}

/**
  * This wrapper wraps another wrapper
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait AwtComponentWrapperWrapper extends ComponentWrapper with AwtComponentRelated
{
	// ABSTRACT	----------------------
	
	protected def wrapped: ComponentLike with AwtComponentRelated
	
	
	// IMPLEMENTED	------------------
	
	override def component = wrapped.component
}
