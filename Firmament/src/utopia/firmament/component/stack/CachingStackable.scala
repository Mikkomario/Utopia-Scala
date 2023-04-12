package utopia.firmament.component.stack

import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.shape.stack.modifier.StackSizeModifier

/**
  * This stackable caches the calculated stack size
  * @author Mikko Hilpinen
  * @since 28.4.2019, Reflection v1+
  */
trait CachingStackable extends Stackable with Constrainable
{
	// ATTRIBUTES	-----------------
	
	// TODO: Make these abstract, also
	private var _constraints = Vector[StackSizeModifier]()
	private val cachedStackSize = ResettableLazy[StackSize] { calculatedStackSizeWithConstraints }
	
	
	// IMPLEMENTED	-----------------
	
	override def constraints = _constraints
	
	override def constraints_=(newConstraints: Vector[StackSizeModifier]) = {
		_constraints = newConstraints
		resetCachedSize()
	}
	
	override def stackSize = cachedStackSize.value
	
	override def resetCachedSize() = cachedStackSize.reset()
}
