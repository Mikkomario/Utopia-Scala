package utopia.reach.component.factory

import utopia.firmament.context.DualFormContext
import utopia.reach.component.factory.FromGenericContextComponentFactoryFactory.Gccff
import utopia.reach.component.hierarchy.ComponentHierarchy

import scala.language.implicitConversions

object FromContextComponentFactoryFactory
{
	// TYPES    -------------------------
	
	/**
	  * Type alias for FromContextComponentFactoryFactory
	  */
	type Ccff[-N, +F] = FromContextComponentFactoryFactory[N, F]
	
	
	// IMPLICIT -------------------------
	
	// Implicitly converts a CFF (component factory -factory) into a CCFF (contextual component factory -factory)
	// Requires the wrapped CFF to yield a factory that can produce a contextual variant of itself
	implicit def wrap[N, CF](ff: ComponentFactoryFactory[_ <: FromContextFactory[N, CF]]): Ccff[N, CF] =
		apply { (hierarchy, context) => ff(hierarchy).withContext(context) }
	
	// A version of 'wrap', which supports generic contextual component factories
	implicit def wrapGeneric[A, Top >: N, N, CF[X <: Top]](ff: A)(implicit f: A => Gccff[Top, CF]): Ccff[N, CF[N]] =
		apply { (hierarchy, context) => f(ff).withContext(hierarchy, context) }
		
	// Allows one to pass a variable context -supporting component factory -factory (CCFF) in a static context
	implicit def variableAcceptingStatic[NS <: DualFormContext[_, NV], NV, CF](ff: Ccff[NV, CF]): Ccff[NS, CF] =
		apply { (hierarchy, staticContext) => ff.withContext(hierarchy, staticContext.toVariableContext) }
	
	
	// OTHER    -------------------------
	
	def apply[N, CF](f: (ComponentHierarchy, N) => CF): FromContextComponentFactoryFactory[N, CF] =
		new _FromContextCff[N, CF](f)
	
	
	// NESTED   -------------------------
	
	private class _FromContextCff[-N, +CF](f: (ComponentHierarchy, N) => CF)
		extends FromContextComponentFactoryFactory[N, CF]
	{
		override def withContext(hierarchy: ComponentHierarchy, context: N): CF = f(hierarchy, context)
	}
}

/**
  * A factory that produces component factories that utilize contextual information
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait FromContextComponentFactoryFactory[-N, +CF]
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
