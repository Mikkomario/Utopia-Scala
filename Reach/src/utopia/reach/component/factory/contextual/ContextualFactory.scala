package utopia.reach.component.factory.contextual

import utopia.firmament.context.{ColorContextLike, ComponentCreationDefaults}
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory}
import utopia.flow.util.logging.Logger
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

object ContextualFactory
{
	// OTHER    -----------------------
	
	/**
	  * Creates a new contextual factory based of a function
	  * @param context Wrapped context
	  * @param f A function for creating the item from the context
	  * @tparam N Type of wrapped context
	  * @tparam F Type of generated factory
	  * @return A new contextual factory
	  */
	def apply[N, F](context: N)(f: N => F): ContextualFactory[N, F] = new _ContextualFactory[N, F](context, f)
	
	
	// EXTENSIONS   -------------------
	
	// WET WET (from GenericContextualFactory), if more occurrences appear, create a separate trait for this
	implicit class ColorContextualDrawableFactory[N <: ColorContextLike[N, _], +F <: CustomDrawableFactory[F]]
	(val f: ContextualFactory[N, F])
		extends AnyVal
	{
		/**
		  * @param background A background color to assign for this component
		  * @return Copy of this factory that creates components with that background color
		  */
		def withBackground(background: Color): F =
			f.mapContext { _.against(background) }.withCustomDrawer(BackgroundDrawer(background))
		/**
		  * Applies the best background color for the current context
		  * @param background     Background color set to apply to this component
		  * @param preferredShade The color shade that is preferred (default = Standard)
		  * @return Copy of this factory with background drawing and modified context
		  */
		def withBackground(background: ColorSet, preferredShade: ColorLevel): F =
			withBackground(f.context.color.preferring(preferredShade)(background))
		/**
		  * Applies the best background color for the current context
		  * @param background Background color set to apply to this component
		  * @return Copy of this factory with background drawing and modified context
		  */
		def withBackground(background: ColorSet): F = withBackground(background, Standard)
		/**
		  * Applies the best background color for the current context
		  * @param background     Role of the background color to apply to this component
		  * @param preferredShade The color shade that is preferred (default = Standard)
		  * @return Copy of this factory with background drawing and modified context
		  */
		def withBackground(background: ColorRole, preferredShade: ColorLevel): F =
			withBackground(f.context.color.preferring(preferredShade)(background))
		/**
		  * Applies the best background color for the current context
		  * @param background Role of the background color to apply to this component
		  * @return Copy of this factory with background drawing and modified context
		  */
		def withBackground(background: ColorRole): F = withBackground(background, Standard)
	}
	
	
	// NESTED   -----------------------
	
	private class _ContextualFactory[N, R](override val context: N, f: N => R) extends ContextualFactory[N, R]
	{
		override def withContext(context: N): R = f(context)
	}
}

/**
  * Common trait for (component) factories that use some kind of component creation context.
  * This trait is suitable for factories that use static context types.
  * If you want to accept and/or propagate various kinds of context types,
  * please use [[GenericContextualFactory]] instead
  *
  * @tparam N Type of context used by this factory
  * @tparam Repr Actual factory implementation type
  *
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ContextualFactory[N, +Repr] extends Any with HasContext[N]
{
	// ABSTRACT -----------------------
	
	/**
	  * @param context A new context to assign
	  * @return A copy of this factory that uses the specified context
	  */
	def withContext(context: N): Repr
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Logging implementation used for component-related logging
	  */
	protected implicit def log: Logger = ComponentCreationDefaults.componentLogger


	// OTHER    -----------------------
	
	/**
	  * @param f A mapping function for the used context
	  * @return A copy of this factory with mapped context
	  */
	def mapContext(f: N => N) = withContext(f(context))
}
