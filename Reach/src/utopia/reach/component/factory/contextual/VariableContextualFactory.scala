package utopia.reach.component.factory.contextual

import utopia.firmament.context.ComponentCreationDefaults
import utopia.flow.util.logging.Logger
import utopia.reach.component.template.PartOfComponentHierarchy

/**
  * Common trait for component factories that use a variable context parameter (i.e. a context pointer)
  * @author Mikko Hilpinen
  * @since 31.5.2023, v1.1
  */
trait VariableContextualFactory[N, +Repr] extends PartOfComponentHierarchy with HasContext[N]
{
	// ABSTRACT --------------------------
	
	/**
	  * @param context A new context to use
	  * @return Copy of this factory that uses the specified context
	  */
	def withContext(context: N): Repr
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Implicit logging implementation used in component-construction
	  */
	implicit protected def log: Logger = ComponentCreationDefaults.componentLogger
	
	
	// OTHER    --------------------------
	
	/**
	  * @param f A mapping function for the component creation context
	  * @return Copy of this factory that uses a mapped context
	  */
	// TODO: May apply a hierarchy, specific mapping condition
	def mapContext(f: N => N) = withContext(f(context))
}
