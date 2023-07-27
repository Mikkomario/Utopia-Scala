package utopia.reach.component.factory.contextual

import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.reach.component.hierarchy.ComponentHierarchy

/**
  * Common trait for component factories that use a variable context parameter (i.e. a context pointer)
  * @author Mikko Hilpinen
  * @since 31.5.2023, v1.1
  */
trait VariableContextualFactory[N, +Repr]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Component hierarchy used when creating the new components
	  */
	protected def parentHierarchy: ComponentHierarchy
	
	/**
	  * @return Pointer that determines the context of the created components
	  */
	protected def contextPointer: Changing[N]
	/**
	  * @param p A new context pointer to use
	  * @return Copy of this factory that uses the specified context pointer
	  */
	def withContextPointer(p: Changing[N]): Repr
	
	
	// OTHER    --------------------------
	
	/**
	  * @param context New (static) context to use in component creation
	  * @return Copy of this factory that uses the specified static context
	  */
	def withContext(context: N): Repr = withContextPointer(Fixed(context))
	/**
	  * @param f A mapping function for the component creation context
	  * @return Copy of this factory that uses a mapped context
	  */
	def mapContext(f: N => N) = withContextPointer(contextPointer.mapWhile(parentHierarchy.linkPointer)(f))
	/**
	  * @param f A mapping function that targets the current component creation context.
	  *          Returns a variable context.
	  * @return Copy of this factory that uses the resulting variable context.
	  */
	def flatMapContext(f: N => Changing[N]) = withContextPointer(contextPointer.flatMap(f))
}
