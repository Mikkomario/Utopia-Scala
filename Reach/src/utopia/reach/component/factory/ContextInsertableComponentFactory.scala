package utopia.reach.component.factory

import scala.language.implicitConversions

/*
object ContextInsertableComponentFactory
{
	// Automatically inserts the implicit context to the available factory, if possible
	implicit def autoInsertContext[N <: Top, Top, F[X <: Top] <: ContextualComponentFactory[X, Top, F]]
	(factory: ContextInsertableComponentFactory[Top, F])(implicit context: N): F[N] = factory.withContext(context)
}*/

/**
  * A factory that can be enriched with component creation context in order to create a contextual component factory
  * @author Mikko Hilpinen
  * @since 12.10.2020, v0.1
  */
trait ContextInsertableComponentFactory[Top, +Contextual[X <: Top] <: ContextualComponentFactory[X, Top, Contextual]]
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
