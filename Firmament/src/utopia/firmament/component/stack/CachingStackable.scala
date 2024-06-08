package utopia.firmament.component.stack

import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.firmament.model.stack.StackSize
import utopia.firmament.model.stack.modifier.StackSizeModifier
import utopia.flow.collection.immutable.Empty

/**
  * This stackable caches the calculated stack size
  * @author Mikko Hilpinen
  * @since 28.4.2019, Reflection v1+
  */
trait CachingStackable extends Stackable with Constrainable
{
	// ATTRIBUTES	-----------------
	
	// TODO: Make these abstract, also
	private var _constraints: Seq[StackSizeModifier] = Empty
	// NB: Rounding is an experimental update added 29.7.2023, v1.1 - remove if not useful
	private val cachedStackSize = ResettableLazy[StackSize] { calculatedStackSizeWithConstraints.round }
	
	
	// IMPLEMENTED	-----------------
	
	override def constraints = _constraints
	
	override def constraints_=(newConstraints: Seq[StackSizeModifier]) = {
		_constraints = newConstraints
		resetCachedSize()
	}
	
	override def stackSize = cachedStackSize.value
	
	override def resetCachedSize() = cachedStackSize.reset()
}
