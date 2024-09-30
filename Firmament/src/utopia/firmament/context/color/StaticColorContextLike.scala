package utopia.firmament.context.color

import utopia.firmament.context.base.StaticBaseContextLike
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * Common trait for static context implementations which specify a container background
  * @author Mikko Hilpinen
  * @since 29.9.2024, v1.3.1
  * @tparam Repr This context type
  * @tparam Textual This context type when textual information is added
  */
trait StaticColorContextLike[+Repr, +Textual]
	extends StaticBaseContextLike[Repr, Repr] with ColorContextCopyable[Repr, Textual]
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Background color of the current container component or other context
	  */
	def background: Color
	/**
	  * @return Color to use in text (and icon) elements
	  */
	def textColor: Color
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Access to colors that should be used in this context.
	  *         Assumes that preferred shading is the default shading and that there is no small text to
	  *         calculate contrast for.
	  */
	def color = ContextualColorAccess(Standard, expectSmallObjects = false)
	
	/**
	  * @return A color to use for text that represents a hint or a disabled element
	  */
	def hintTextColor = textColor.timesAlpha(0.625)
	
	
	// IMPLEMENTED    -------------------------
	
	override def backgroundPointer = Fixed(background)
	override def textColorPointer = Fixed(textColor)
	override def hintTextColorPointer = Fixed(hintTextColor)
	
	override def colorPointer: ColorAccess[Changing[Color]] = color.map { Fixed(_) }
	
	/**
	  * @param color New background color (set) to assume
	  * @param preferredShade Preferred color shade (default = standard)
	  * @return A copy of this context with a background from the specified set,
	  *         which is suited against the current context
	  */
	def withBackground(color: ColorSet, preferredShade: ColorLevel): Repr =
		against(color.against(background, preferredShade))
	/**
	  * @param role          New background color (role) to assume
	  * @param preferredShade Preferred color shade (default = standard)
	  * @return A copy of this context with a background from the specified color type,
	  *         which is suited against the current context
	  */
	def withBackground(role: ColorRole, preferredShade: ColorLevel): Repr =
		withBackground(colors(role).against(background, preferredShade))
	
	/**
	  * @param f A mapping function to apply for the current background color
	  * @return A copy of this context with modified background
	  */
	def mapBackground(f: Color => Color) = withBackground(f(background))
	/**
	  * @param f A mapping function for the current text color
	  * @return A copy of this context with mapped text color
	  */
	def mapTextColor(f: Color => Color) = withTextColor(f(textColor))
	
	
	// NESTED   -------------------------
	
	case class ContextualColorAccess(level: ColorLevel, expectSmallObjects: Boolean)
		extends ColorAccess[Color] with ColorAccessLike[Color, ContextualColorAccess]
	{
		// COMPUTED ---------------------
		
		/**
		  * @return Minimum contrast used within this setting
		  */
		def minimumContrast =
			if (expectSmallObjects) contrastStandard.defaultMinimumContrast else contrastStandard.largeTextMinimumContrast
		
		
		// IMPLEMENTED  -----------------
		
		/**
		  * @return Access to colors when small objects need to be recognized
		  */
		def expectingSmallObjects =
			if (expectSmallObjects) this else copy(expectSmallObjects = true)
		/**
		  * @return Access to colors when only large objects need to be recognized
		  */
		def expectingLargeObjects =
			if (expectSmallObjects) copy(expectSmallObjects = false) else this
		/**
		  * @return Access to colors where contrast is suitable for the current font settings
		  */
		def forText = copy(expectSmallObjects = !font.isLargeOnScreen)
		
		/**
		  * @param color A proposed set of colors
		  * @return The best color from the specified set for this context
		  */
		def apply(color: ColorSet) = color.against(background, level, minimumContrast)
		/**
		  * @param role A color role
		  * @return Color to use for that role in this context
		  */
		def apply(role: ColorRole) = colors(role).against(background, level, minimumContrast)
		/**
		  * @param role A color role
		  * @param competingColor A color the resulting color should not resemble
		  * @param moreColors More excluded colors
		  * @return A color of the specified role that is as different as possible
		  *         from the specified colors and the current background color
		  */
		def differentFrom(role: ColorRole, competingColor: Color, moreColors: Color*) =
			colors(role).againstMany(moreColors.toSet + competingColor + background)
		
		/**
		  * @param level A color level
		  * @return Access that prefers that color level
		  */
		def preferring(level: ColorLevel) = copy(level = level)
	}
}
