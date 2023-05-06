package utopia.reach.component.factory

import utopia.firmament.context.ColorContextLike
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory}
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

object GenericContextualFactory
{
	// EXTENSIONS	------------------------
	
	/*
	trait FillableGenericContextualFactory[+N, Top, Repr[N2 <: Top]]
		extends GenericContextualFactory[N, Top, Repr] with CustomDrawableFactory[Repr]
	 */
	/*
	// Works in IntelliJ builder but not in Scala builder
	implicit class FillableGenericContextualFactory[N1 <: BaseContextLike[_, N2], N2 <: Top, Top >: N1,
		F[X <: Top] <: CustomDrawableFactory[F[X]]](val f: GenericContextualFactory[N1, Top, F]) extends AnyVal
	{
		def withBackground(background: Color) = {
			f.mapContext { _.against(background) }.withCustomDrawer(BackgroundDrawer(background))
		}
	}*/
	// Works for all context types except base context
	// The base context version is too hard for the compiler to understand (at 5.5.2023)
	implicit class GenericColorContextualFactory[N <: ColorContextLike[N, _], Top >: N,
		F[N2 <: Top] <: CustomDrawableFactory[F[N2]]](val f: GenericContextualFactory[N, Top, F]) extends AnyVal
	{
		/**
		  * @param background Background color to apply to this component
		  * @return Copy of this factory with background drawing and modified context
		  */
		def withBackground(background: Color): F[N] =
			f.mapContext { _.against(background) }.withCustomDrawer(BackgroundDrawer(background))
		/**
		  * Applies the best background color for the current context
		  * @param background Background color set to apply to this component
		  * @param preferredShade The color shade that is preferred (default = Standard)
		  * @return Copy of this factory with background drawing and modified context
		  */
		def withBackground(background: ColorSet, preferredShade: ColorLevel): F[N] =
			withBackground(f.context.color.preferring(preferredShade)(background))
		/**
		  * Applies the best background color for the current context
		  * @param background Background color set to apply to this component
		  * @return Copy of this factory with background drawing and modified context
		  */
		def withBackground(background: ColorSet): F[N] = withBackground(background, Standard)
		/**
		  * Applies the best background color for the current context
		  * @param background Role of the background color to apply to this component
		  * @param preferredShade The color shade that is preferred (default = Standard)
		  * @return Copy of this factory with background drawing and modified context
		  */
		def withBackground(background: ColorRole, preferredShade: ColorLevel): F[N] =
			withBackground(f.context.color.preferring(preferredShade)(background))
		/**
		  * Applies the best background color for the current context
		  * @param background Role of the background color to apply to this component
		  * @return Copy of this factory with background drawing and modified context
		  */
		def withBackground(background: ColorRole): F[N] = withBackground(background, Standard)
	}
	
	/*
	implicit class BaseContextComponentFactory[N <: BaseContext, Repr[_]]
	(val f: ContextualComponentFactory[N, _ >: BaseContext, Repr])
		extends AnyVal with BaseContextWrapper[Repr[_ >: N], Repr[_ >: N]]
	{
		override def self: Repr[N] = f.withContext(f.context)
		
		override def wrapped = f.context
		
		override def withBase(baseContext: BaseContext) = f.withContext(baseContext)
		override def against(background: Color): Repr[N] = f.mapContext { _.against(background) }
		
		override def *(mod: Double): Repr[N] = f.mapContext { _ * mod }
	}
	 */
	
	/*
	implicit class TextContextComponentFactory[Repr[_]]
	(val f: GenericContextualFactory[_ <: TextContext, _ >: TextContext, Repr])
		extends TextContextWrapper[Repr[TextContext]]
	{
		override def self: Repr[TextContext] = f.withContext(f.context)
		override def textContext: TextContext = f.context
		
		override def withTextContext(base: TextContext): Repr[TextContext] = f.withContext(base)
		override def *(mod: Double): Repr[TextContext] = f.mapContext { _ * mod }
	}*/
}

/**
  * A common trait for component factories that use a generic component creation context
  * @author Mikko Hilpinen
  * @since 12.10.2020, v0.1
  * @tparam N Type of context used by this factory
  * @tparam Top The type limit of accepted context parameters
  * @tparam Repr Implementation type of this factory (generic)
  */
trait GenericContextualFactory[+N, Top, +Repr[N2 <: Top]]
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return Component creation context used inside this factory
	  */
	def context: N
	
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
