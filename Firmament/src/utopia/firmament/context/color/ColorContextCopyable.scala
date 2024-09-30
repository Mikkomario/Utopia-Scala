package utopia.firmament.context.color

import utopia.firmament.context.base.BaseContextCopyable
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * Common trait for color context instances which provide copy functions.
  * Does not make assumptions about this context's static or variable nature.
  * @author Mikko Hilpinen
  * @since 29.9.2024, v1.3.1
  * @tparam Repr This context type
  * @tparam Textual This context type when textual information is added
  */
trait ColorContextCopyable[+Repr, +Textual] extends ColorContextPropsView with BaseContextCopyable[Repr, Repr]
{
	// ABSTRACT	------------------------
	
	/**
	  * @return A copy of this context that uses the default text color
	  *         (typically black or white, depending on the context background)
	  */
	def withDefaultTextColor: Repr
	
	/**
	  * @return A copy of this context that is suitable for creating components that display text
	  */
	def forTextComponents: Textual
	
	/**
	  * @param color New text color to use
	  * @return A copy of this context with that text color being used
	  */
	def withTextColor(color: Color): Repr
	/**
	  * @param color New set of text colors to use
	  * @return A copy of this context with that text color (set) being used
	  */
	def withTextColor(color: ColorSet): Repr
	
	/**
	  * @param color New background color (set) to assume
	  * @param preferredShade Preferred color shade (default = standard)
	  * @return A copy of this context with a background from the specified set,
	  *         which is suited against the current context
	  */
	def withBackground(color: ColorSet, preferredShade: ColorLevel): Repr
	/**
	  * @param role          New background color (role) to assume
	  * @param preferredShade Preferred color shade (default = standard)
	  * @return A copy of this context with a background from the specified color type,
	  *         which is suited against the current context
	  */
	def withBackground(role: ColorRole, preferredShade: ColorLevel): Repr
	
	/**
	  * @param f A mapping function to apply for the current background color
	  * @return A copy of this context with modified background
	  */
	def mapBackground(f: Color => Color): Repr
	/**
	  * @param f A mapping function for the current text color
	  * @return A copy of this context with mapped text color
	  */
	def mapTextColor(f: Color => Color): Repr
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return A copy of this context that uses a highlighted (brightened or darkened) background color
	  */
	def withHighlightedBackground = mapBackground { _.highlighted }
	
	
	// OTHER    -------------------------
	
	/**
	  * @param role Color role to use
	  * @return A copy of this context that uses text color matching that role
	  */
	def withTextColor(role: ColorRole): Repr = withTextColor(colors(role))
	
	/**
	  * @param background New background color to assume
	  * @return A copy of this context with that background color
	  */
	def withBackground(background: Color) = against(background)
	/**
	  * @param color          New background color (set) to assume
	  * @return A copy of this context with a background from the specified set,
	  *         which is suited against the current context
	  */
	def withBackground(color: ColorSet): Repr = withBackground(color, Standard)
	/**
	  * @param role           New background color (role) to assume
	  * @return A copy of this context with a background from the specified color type,
	  *         which is suited against the current context
	  */
	def withBackground(role: ColorRole): Repr = withBackground(role, Standard)
	
	/**
	  * Alias for withBackgroundColor(Color)
	  * @param color New background color to assume
	  * @return A copy of this context with the specified background color
	  */
	def /(color: Color) = withBackground(color)
	/**
	  * @param color New background color (set) to assume
	  * @return A copy of this context with a background from the specified set,
	  *         which is suited against the current context
	  */
	def /(color: ColorSet) = withBackground(color)
	/**
	  * @param color New background color (role) to assume
	  * @return A copy of this context with a background from the specified color type,
	  *         which is suited against the current context
	  */
	def /(color: ColorRole) = withBackground(color)
	/**
	  * @param color New background color (role) to assume + preferred color shade
	  * @return A copy of this context with a background from the specified color type,
	  *         which is suited against the current context
	  */
	def /(color: (ColorRole, ColorLevel)) = withBackground(color._1, color._2)
}
