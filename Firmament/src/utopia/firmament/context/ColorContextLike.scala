package utopia.firmament.context

import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.ColorRole._
import utopia.paradigm.color.ColorShade._
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * A common trait for contexts that contain container color information
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  * @tparam Repr This context type
  * @tparam Textual This context type when textual information is added
  */
trait ColorContextLike[+Repr, +Textual] extends BaseContextWrapper[Repr, Repr]
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
	
	/**
	  * @return A copy of this context that uses a highlighted (brightened or darkened) background color
	  */
	def withHighlightedBackground = withBackground(background.highlighted)
	
	@deprecated("Please use .background instead", "v1.0")
	def containerBackground = background
	
	
	// OTHER    -------------------------
	
	/**
	  * @param background New background color to assume
	  * @return A copy of this context with that background color
	  */
	def withBackground(background: Color) = against(background)
	/**
	  * @param color New background color (set) to assume
	  * @param preferredShade Preferred color shade (default = standard)
	  * @return A copy of this context with a background from the specified set,
	  *         which is suited against the current context
	  */
	def withBackground(color: ColorSet, preferredShade: ColorLevel): Repr =
		against(color.against(background, preferredShade))
	/**
	  * @param color          New background color (set) to assume
	  * @return A copy of this context with a background from the specified set,
	  *         which is suited against the current context
	  */
	def withBackground(color: ColorSet): Repr = withBackground(color, Standard)
	/**
	  * @param role          New background color (role) to assume
	  * @param preferredShade Preferred color shade (default = standard)
	  * @return A copy of this context with a background from the specified color type,
	  *         which is suited against the current context
	  */
	def withBackground(role: ColorRole, preferredShade: ColorLevel): Repr =
		withBackground(colors(role).against(background, preferredShade))
	/**
	  * @param role           New background color (role) to assume
	  * @return A copy of this context with a background from the specified color type,
	  *         which is suited against the current context
	  */
	def withBackground(role: ColorRole): Repr = withBackground(role, Standard)
	
	/**
	  * @param role Color role to use
	  * @return A copy of this context that uses text color matching that role
	  */
	def withTextColor(role: ColorRole): Repr = withTextColor(colors(role))
	
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
	{
		/**
		  * @return Access to light colors
		  */
		def light = preferring(Light)
		/**
		  * @return Access to dark colors
		  */
		def dark = preferring(Dark)
		
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
		  * @return Minimum contrast used within this setting
		  */
		def minimumContrast =
			if (expectSmallObjects) contrastStandard.defaultMinimumContrast else contrastStandard.largeTextMinimumContrast
		
		/**
		  * @return Gray color to use
		  */
		def gray = apply(Gray)
		
		/**
		  * @return Primary color to use
		  */
		def primary = apply(Primary)
		/**
		  * @return Secondary color to use
		  */
		def secondary = apply(Secondary)
		/**
		  * @return Tertiary color to use
		  */
		def tertiary = apply(Tertiary)
		
		/**
		  * @return Color to use to represent success
		  */
		def success = apply(Success)
		/**
		  * @return Color to use to represent an error situation or a failure
		  */
		def failure = apply(Failure)
		/**
		  * @return Color to use to represent a warning or danger
		  */
		def warning = apply(Warning)
		/**
		  * @return Color to use to represent additional information or notifications
		  */
		def info = apply(Info)
		
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
