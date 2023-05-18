package utopia.reach.component.factory

import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromGenericContextComponentFactoryFactory.Gccff
import utopia.reach.component.factory.contextual.GenericContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy

object Mixed extends Cff[Mixed]

/**
  * A factory for creating all kinds of component factories
  * @author Mikko Hilpinen
  * @since 11.10.2020, v0.1
  */
case class Mixed(parentHierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[Any, ContextualMixed]
{
	override def withContext[N](context: N) = ContextualMixed(parentHierarchy, context)
	
	/**
	  * @param factoryFactory A component factory factory
	  * @tparam F Type of component factory
	  * @return A specific type of component factory that uses this same hierarchy
	  */
	def apply[F](factoryFactory: ComponentFactoryFactory[F]) = factoryFactory(parentHierarchy)
}

case class ContextualMixed[N](parentHierarchy: ComponentHierarchy, context: N)
	extends GenericContextualFactory[N, Any, ContextualMixed]
{
	// COMPUTED	-------------------------------
	
	/**
	  * @return A copy of this factory without any contextual information
	  */
	def withoutContext = Mixed(parentHierarchy)
	
	
	// IMPLEMENTED	---------------------------
	
	override def withContext[C2](newContext: C2) = copy(context = newContext)
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param factoryFactory A component factory factory
	  * @tparam F Type of component factory
	  * @return A specific type of component factory that uses this same hierarchy and context
	  */
	def generic[F[X <: N]](factoryFactory: Gccff[N, F]) = factoryFactory.withContext(parentHierarchy, context)
	/**
	  * @param ff A component factory factory
	  * @tparam F Type of contextual component factory
	  * @return A contextual component factory from the specified factory that uses the context from this item
	  */
	def apply[F](ff: FromContextComponentFactoryFactory[N, F]) = ff.withContext(parentHierarchy, context)
}