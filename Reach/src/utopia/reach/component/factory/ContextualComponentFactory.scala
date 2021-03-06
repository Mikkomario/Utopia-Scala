package utopia.reach.component.factory

import utopia.reflection.component.context.TextContext
import utopia.reflection.shape.Alignment
import utopia.reflection.text.Font

object ContextualComponentFactory
{
	// EXTENSIONS	------------------------
	
	implicit class TextContextComponentFactory[N <: TextContext, T >: TextContext, Repr[N2 <: T]]
	(val f: ContextualComponentFactory[N, T, Repr]) extends AnyVal
	{
		/**
		  * @param alignment New text alignment
		  * @return A copy of this factory that uses specified text alignment
		  */
		def withAlignment(alignment: Alignment) = f.mapContext { _.withTextAlignment(alignment) }
		
		/**
		  * @param font New text font
		  * @return A copy of this factory that uses the specified text font
		  */
		def withFont(font: Font) = f.mapContext { _.withFont(font) }
		
		/**
		  * @param scaling A font scaling factor (1 keeps the font as is)
		  * @return A copy of this factory with font scaled by specified amount
		  */
		def withScaledFont(scaling: Double) = f.mapContext { _.mapFont { _ * scaling } }
	}
}

/**
  * A common trait for component factories that use a component creation context
  * @author Mikko Hilpinen
  * @since 12.10.2020, v0.1
  * @tparam N Type of context used by this factory
  * @tparam Top The type limit of accepted context parameters
  * @tparam Repr Implementation type of this factory (generic)
  */
trait ContextualComponentFactory[+N, Top, +Repr[N2 <: Top]]
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
