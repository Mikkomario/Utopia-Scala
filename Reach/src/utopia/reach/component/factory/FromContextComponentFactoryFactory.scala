package utopia.reach.component.factory

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
	
	implicit def wrap[N, CF](ff: ComponentFactoryFactory[_ <: FromContextFactory[N, CF]]): Ccff[N, CF] =
		apply { (hierarchy, context) => ff(hierarchy).withContext(context) }
	
	implicit def wrapGeneric2[A, Top >: N, N, CF[X <: Top]](ff: A)(implicit f: A => Gccff[Top, CF]): Ccff[N, CF[N]] =
		apply { (hierarchy, context) => f(ff).withContext(hierarchy, context) }
	/*
	implicit def wrapGeneric[Top >: N, N, CF[X <: Top]](ff: Cff[_ <: FromGenericContextFactory[Top, CF]]): Ccff[N, CF[N]] =
		apply[N, CF[N]] { (hierarchy, context) => ff.apply(hierarchy).withContext(context) }
	*/
	
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
