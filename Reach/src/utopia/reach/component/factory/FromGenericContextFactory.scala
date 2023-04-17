package utopia.reach.component.factory

/**
  * A factory that can be enriched with component creation context in order to create a contextual component factory.
  * Suppports generic context types.
  * @author Mikko Hilpinen
  * @since 12.10.2020, v0.1
  * @tparam Top The highest accepted context class
  * @tparam Contextual A contextual type of this factory (i.e. the result type of this factory)
  */
trait FromGenericContextFactory[-Top, +Contextual[X <: Top]]
{
	// ABSTRACT	-----------------------------
	
	/**
	  * @param context A component creation context
	  * @tparam N Type of the component creation context
	  * @return A new component factory that will use the specified context
	  */
	def withContext[N <: Top](context: N): Contextual[N]
	
	
	// COMPUTED	-----------------------------
	
	/**
	  * @param context Implicit component creation context
	  * @tparam N Type of component creation context
	  * @return A new contextual component creation factory that uses the implicitly available context
	  */
	def contextual[N <: Top](implicit context: N): Contextual[N] = withContext(context)
}
