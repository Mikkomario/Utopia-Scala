package utopia.reach.component.factory

import utopia.flow.view.template.eventful.Changing
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromGenericContextComponentFactoryFactory.Gccff
import utopia.reach.component.factory.contextual.GenericContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.PartOfComponentHierarchy

object Mixed extends Cff[Mixed]

/**
  * A factory for creating all kinds of component factories
  * @author Mikko Hilpinen
  * @since 11.10.2020, v0.1
  */
case class Mixed(hierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[Any, ContextualMixed] with PartOfComponentHierarchy
{
	override def withContext[N](context: N) = ContextualMixed(hierarchy, context)
	@deprecated("Deprecated for removal. With the addition of variable context classes, this should not be necessary anymore", "v1.5")
	def withContextPointer[N](p: Changing[N]) = VariableContextualMixed(hierarchy, p)
	
	/**
	  * @param factoryFactory A component factory factory
	  * @tparam F Type of component factory
	  * @return A specific type of component factory that uses this same hierarchy
	  */
	def apply[F](factoryFactory: ComponentFactoryFactory[F]) = factoryFactory(hierarchy)
}

case class ContextualMixed[+N](hierarchy: ComponentHierarchy, context: N)
	extends GenericContextualFactory[N, Any, ContextualMixed] with PartOfComponentHierarchy
{
	// COMPUTED	-------------------------------
	
	/**
	  * @return A copy of this factory without any contextual information
	  */
	def withoutContext = Mixed(hierarchy)
	
	
	// IMPLEMENTED	---------------------------
	
	override def withContext[C2](newContext: C2) = copy(context = newContext)
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param factoryFactory A component factory -factory
	  * @tparam N2 Type of context accepted by the specified factory
	  * @tparam F Type of component factory
	  * @return A specific type of component factory that uses this same hierarchy and context
	  */
	def generic[N2 >: N, F[X <: N2]](factoryFactory: Gccff[N2, F]): F[N2] =
		factoryFactory.withContext(hierarchy, context)
	/**
	  * @param ff A component factory -factory
	  * @tparam F Type of contextual component factory
	  * @return A contextual component factory from the specified factory that uses the context from this item
	  */
	def apply[F](ff: FromContextComponentFactoryFactory[N, F]): F = ff.withContext(hierarchy, context)
}

@deprecated("Deprecated for removal. With the addition of variable context classes, this should not be necessary anymore", "v1.5")
case class VariableContextualMixed[N](hierarchy: ComponentHierarchy, contextPointer: Changing[N])
	extends PartOfComponentHierarchy
{
	def withoutContext = Mixed(hierarchy)
	
	def withContextPointer[N2](p: Changing[N2]) = copy(contextPointer = p)
	def withContext[N2](context: N2) = ContextualMixed(hierarchy, context)
	
	def apply[F](ff: FromVariableContextComponentFactoryFactory[N, F]) =
		ff.withContextPointer(hierarchy, contextPointer)
	def static[F](ff: FromContextComponentFactoryFactory[N, F]) =
		ff.withContext(hierarchy, contextPointer.value)
}