package utopia.reflection.component.stack

/**
  * This trait is implemented by components that don't wrap or contain any other stackable instances,
  * by components that represent "leaves" in the stack hierarchy
  * @author Mikko Hilpinen
  * @since 13.3.2020, v1
  */
trait StackLeaf extends Stackable
{
	// ATTRIBUTES	-----------------------
	
	override var isAttachedToMainHierarchy = false
}
