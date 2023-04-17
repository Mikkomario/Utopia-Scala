package utopia.reach.component.factory

object ContextualFactory
{
	// OTHER    -----------------------
	
	/**
	  * Creates a new contextual factory based of a function
	  * @param context Wrapped context
	  * @param f A function for creating the item from the context
	  * @tparam N Type of wrapped context
	  * @tparam F Type of generated factory
	  * @return A new contextual factory
	  */
	def apply[N, F](context: N)(f: N => F): ContextualFactory[N, F] = new _ContextualFactory[N, F](context, f)
	
	
	// NESTED   -----------------------
	
	private class _ContextualFactory[N, R](override val context: N, f: N => R) extends ContextualFactory[N, R]
	{
		override def withContext(context: N): R = f(context)
	}
}

/**
  * Common trait for (component) factories that use some kind of component creation context.
  * This trait is suitable for factories that use static context types.
  * If you want to accept and/or propagate various kinds of context types,
  * please use [[GenericContextualFactory]] instead
  *
  * @tparam N Type of context used by this factory
  * @tparam Repr Actual factory implementation type
  *
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ContextualFactory[N, +Repr]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The context used by this factory
	  */
	def context: N
	
	/**
	  * @param context A new context to assign
	  * @return A copy of this factory that uses the specified context
	  */
	def withContext(context: N): Repr


	// OTHER    -----------------------
	
	/**
	  * @param f A mapping function for the used context
	  * @return A copy of this factory with mapped context
	  */
	def mapContext(f: N => N) = withContext(f(context))
}
