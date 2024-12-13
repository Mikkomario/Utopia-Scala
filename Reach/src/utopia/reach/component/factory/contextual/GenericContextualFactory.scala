package utopia.reach.component.factory.contextual

import utopia.firmament.context.{DualFormContext, HasContext}
import utopia.firmament.context.color.StaticColorContextLike
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory}
import utopia.paradigm.color.Color

object GenericContextualFactory
{
	// EXTENSIONS	------------------------
	
	// Allows easier conversions between static and variable contexts
	implicit class GenericDualFormContextualFactory[+N <: DualFormContext[NS, NV], NS <: Top, NV <: Top, Top, +Repr[N2 <: Top]]
	(val f: GenericContextualFactory[N, Top, Repr])
		extends AnyVal
	{
		/**
		  * @return Copy of this factory utilizing the current context's "snapshot".
		  *         If this factory is using / wrapping a variable context instance,
		  *         the returned factory only uses the current state of that context,
		  *         captured as a static context instance.
		  */
		def withCurrentContext = f.mapContext { _.current }
		/**
		  * @return Copy of this factory which supports variable context components.
		  */
		def variable = f.mapContext { _.toVariableContext }
	}
	
	// TODO: This class doesn't implement automatic repaint. Consider removing or replacing this at some point.
	// Works for all context types except base context
	// The base context version is too hard for the compiler to understand (at 5.5.2023)
	implicit class GenericColorContextualFactory[N <: StaticColorContextLike[N, _], Top >: N,
		F[N2 <: Top] <: CustomDrawableFactory[F[N2]]](val f: GenericContextualFactory[N, Top, F])
		extends AnyVal with StaticContextualBackgroundAssignable[N, F[N]]
	{
		// IMPLEMENTED  -----------------------
		
		override def context: N = f.context
		
		/**
		  * @param background Background color to apply to this component
		  * @return Copy of this factory with background drawing and modified context
		  */
		override def withBackground(background: Color): F[N] =
			f.mapContext { _.against(background) }.withCustomDrawer(BackgroundDrawer(background))
	}
}

/**
  * A common trait for component factories that use a generic component creation context
  * @author Mikko Hilpinen
  * @since 12.10.2020, v0.1
  * @tparam N Type of context used by this factory
  * @tparam Top The type limit of accepted context parameters
  * @tparam Repr Implementation type of this factory (generic)
  */
trait GenericContextualFactory[+N, Top, +Repr[N2 <: Top]] extends HasContext[N]
{
	// ABSTRACT	----------------------------
	
	/**
	  * @param newContext A new component creation context
	  * @tparam N2 Type of the new context
	  * @return A copy of this factory that uses the specified context
	  */
	def withContext[N2 <: Top](newContext: N2): Repr[N2]
	
	
	// OTHER	----------------------------
	
	/**
	  * @param f A context mapping function
	  * @tparam N2 Type of the new context
	  * @return A copy of this factory that uses the mapped context
	  */
	def mapContext[N2 <: Top](f: N => N2) = withContext(f(context))
}
