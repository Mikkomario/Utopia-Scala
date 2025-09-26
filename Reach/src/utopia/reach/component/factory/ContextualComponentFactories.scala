package utopia.reach.component.factory

import utopia.firmament.context.DualFormContext
import utopia.reach.component.factory.GenericContainerFactories.GCF
import utopia.reach.component.hierarchy.ComponentHierarchy

import scala.language.implicitConversions

object ContextualComponentFactories
{
	// TYPES    -------------------------
	
	/**
	 * Type alias for [[ContextualComponentFactories]]
	 */
	type CCF[-N, +F] = ContextualComponentFactories[N, F]
	/**
	  * Type alias for FromContextComponentFactoryFactory
	  */
	@deprecated("Renamed to CCF", "v1.7")
	type Ccff[-N, +F] = ContextualComponentFactories[N, F]
	
	
	// IMPLICIT -------------------------
	
	// Implicitly converts a CF (component factories) into a CCF (contextual component factories)
	// Requires the wrapped CF to yield a factory that can produce a contextual variant of itself
	implicit def wrap[N, CF](ff: ComponentFactories[_ <: FromContextFactory[N, CF]]): CCF[N, CF] =
		apply { (hierarchy, context) => ff(hierarchy).withContext(context) }
	
	// A version of 'wrap', which supports generic contextual component factories
	implicit def wrapGeneric[A, Top >: N, N, CF[X <: Top]](ff: A)(implicit f: A => GCF[Top, CF]): CCF[N, CF[N]] =
		apply { (hierarchy, context) => f(ff).withContext(hierarchy, context) }
		
	// Allows one to pass a variable context -supporting component factory -factory (CCFF) in a static context
	implicit def variableAcceptingStatic[NS <: DualFormContext[_, NV], NV, CF](ff: CCF[NV, CF]): CCF[NS, CF] =
		apply { (hierarchy, staticContext) => ff.withContext(hierarchy, staticContext.toVariableContext) }
	
	
	// OTHER    -------------------------
	
	def apply[N, CF](f: (ComponentHierarchy, N) => CF): ContextualComponentFactories[N, CF] =
		new _ContextualComponentFactories[N, CF](f)
	
	
	// NESTED   -------------------------
	
	private class _ContextualComponentFactories[-N, +CF](f: (ComponentHierarchy, N) => CF)
		extends ContextualComponentFactories[N, CF]
	{
		override def withContext(hierarchy: ComponentHierarchy, context: N): CF = f(hierarchy, context)
	}
}

/**
  * A factory that produces component factories that utilize contextual information
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ContextualComponentFactories[-N, +CF]
{
	// ABSTRACT -----------------------
	
	/**
	  * @param hierarchy Component hierarchy to use
	  * @param context Component creation context to use
	  * @return A new component creation factory that uses the specified context
	  */
	def withContext(hierarchy: ComponentHierarchy, context: N): CF
	
	
	// COMPUTED ----------------------
	
	/**
	  * @param hierarchy Component hierarchy to use
	  * @param context   Component creation context to use
	  * @return A new component creation factory that uses the specified context
	  */
	def contextual(hierarchy: ComponentHierarchy)(implicit context: N) = withContext(hierarchy, context)
}
