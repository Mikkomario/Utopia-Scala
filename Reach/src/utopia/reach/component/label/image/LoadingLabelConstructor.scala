package utopia.reach.component.label.image

import utopia.firmament.component.stack.Constrainable
import utopia.reach.component.template.ReachComponent

object LoadingLabelConstructor
{
	/**
	  * @param f A constructor function to wrap. Accepts an initialized label factory and constructs the label.
	  * @return A new loading label constructor wrapping the specified function.
	  */
	implicit def apply(f: AnimatedImageLabelFactory => ReachComponent with Constrainable): LoadingLabelConstructor =
		new _LoadingLabelConstructor(f)
		
	
	// NESTED   --------------------------------
	
	private class _LoadingLabelConstructor(f: AnimatedImageLabelFactory => ReachComponent with Constrainable)
		extends LoadingLabelConstructor
	{
		override def apply(factory: AnimatedImageLabelFactory) = f(factory)
	}
	
}

/**
  * An implicitly passed constructor that defines how loading labels are formed
  * @author Mikko Hilpinen
  * @since 10.02.2025, v1.6
  */
trait LoadingLabelConstructor
{
	/**
	  * Constructs a new loading label
	  * @param factory An initialized factory for constructing the label
	  * @return A new loading label
	  */
	def apply(factory: AnimatedImageLabelFactory): ReachComponent with Constrainable
}
