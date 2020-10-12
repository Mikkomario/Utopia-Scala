package utopia.reflection.component.reach.factory

import scala.language.implicitConversions

import utopia.reflection.component.context.BaseContextLike

object ContextualComponentFactoryFactory
{
	// Automatically inserts the implicit context to the available factory, if possible
	implicit def autoInsertContext[C <: BaseContextLike, C2 <: C, F[X <: C] <: ContextualComponentFactory[X, C, F]]
	(factory: ContextualComponentFactoryFactory[C, F])(implicit context: C2): F[C2] = factory.withContext(context)
}

/**
  * A factory used for creating contextual component factories
  * @author Mikko Hilpinen
  * @since 12.10.2020, v2
  */
trait ContextualComponentFactoryFactory[
	Context <: BaseContextLike, +Factory[X <: Context] <: ContextualComponentFactory[X, Context, Factory]]
{
	// ABSTRACT	-----------------------------
	
	/**
	  * @param context A component creation context
	  * @tparam C2 Type of the component creation context
	  * @return A new component factory that will use the specified context
	  */
	def withContext[C2 <: Context](context: C2): Factory[C2]
	
	
	// COMPUTED	-----------------------------
	
	/**
	  * @param context Implicit component creation context
	  * @tparam C2 Type of component creation context
	  * @return A new contextual component creation factory that uses the implicitly available context
	  */
	def contextual[C2 <: Context](implicit context: C2): Factory[C2] = withContext(context)
}
