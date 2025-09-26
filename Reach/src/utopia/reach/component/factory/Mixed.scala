package utopia.reach.component.factory

import utopia.firmament.context.text.{StaticTextContext, VariableTextContext}
import utopia.reach.component.factory.ComponentFactories.CF
import utopia.reach.component.factory.GenericContainerFactories.GCF
import utopia.reach.component.factory.contextual.GenericContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.PartOfComponentHierarchy

object Mixed extends CF[Mixed]
{
	// TYPES    ---------------------------
	
	/**
	 * An alias for [[ContextualMixed]] of [[StaticTextContext]]
	 */
	type TF = ContextualMixed[StaticTextContext]
	/**
	 * An alias for [[ContextualMixed]] of [[VariableTextContext]]
	 */
	type VTF = ContextualMixed[VariableTextContext]
}

/**
  * A factory for creating all kinds of component factories
  * @author Mikko Hilpinen
  * @since 11.10.2020, v0.1
  */
case class Mixed(hierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[Any, ContextualMixed] with PartOfComponentHierarchy
{
	// IMPLEMENTED  -----------------------
	
	override def withContext[N](context: N) = ContextualMixed(hierarchy, context)
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param factories A component factory factory
	  * @tparam F Type of component factory
	  * @return A specific type of component factory that uses this same hierarchy
	  */
	def apply[F](factories: ComponentFactories[F]) = factories(hierarchy)
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
	  * @param factories A component factory -factory
	  * @tparam N2 Type of context accepted by the specified factory
	  * @tparam F Type of component factory
	  * @return A specific type of component factory that uses this same hierarchy and context
	  */
	def generic[N2 >: N, F[X <: N2]](factories: GCF[N2, F]): F[N2] = factories.withContext(hierarchy, context)
	/**
	  * @param factories A component factory -factory
	  * @tparam F Type of contextual component factory
	  * @return A contextual component factory from the specified factory that uses the context from this item
	  */
	def apply[F](factories: ContextualComponentFactories[N, F]): F = factories.withContext(hierarchy, context)
}