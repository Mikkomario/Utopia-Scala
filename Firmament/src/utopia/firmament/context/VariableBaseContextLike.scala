package utopia.firmament.context

import utopia.firmament.model.Margins
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.text.Font
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorScheme}
import utopia.paradigm.enumeration.ColorContrastStandard

/**
  * Common trait for pointer-based context implementations that provide access to the basic context properties
  * @author Mikko Hilpinen
  * @since 27.09.2024, v1.3.1
  */
trait VariableBaseContextLike[+Repr, +ColorSensitive] extends Any with BaseContextLike[Repr, ColorSensitive]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return Pointer that contains the currently used (default) font
	  */
	def fontPointer: Changing[Font]
	/**
	  * @return Pointer that contains the currently used color scheme
	  */
	def colorsPointer: Changing[ColorScheme]
	/**
	  * @return Pointer that contains the currently applied color contrast standard
	  */
	def contrastStandardPointer: Changing[ColorContrastStandard]
	/**
	  * @return Pointer that contains the currently applied margins size
	  */
	def marginsPointer: Changing[Margins]
	/**
	  * @return Pointer that contains the currently applied stack margin length
	  */
	def stackMarginPointer: Changing[StackLength]
	/**
	  * @return Pointer that contains the currently applied small stack margin length
	  */
	def smallStackMarginPointer: Changing[StackLength]
	/**
	  * @return Pointer that contains true while image-upscaling is enabled
	  */
	def allowImageUpscalingFlag: Flag
	
	/**
	  * @return Cache that contains color scheme map results for various color roles
	  */
	def colorPointersCache: Cache[(ColorRole, ColorLevel), Changing[Color]]
	
	/**
	  * @param p New font pointer
	  * @return Copy of this context with the specified font pointer used
	  */
	def withFontPointer(p: Changing[Font]): Repr
	/**
	  * @param p New color scheme -pointer
	  * @return Copy of this context with the specified pointer used
	  */
	def withColorsPointer(p: Changing[ColorScheme]): Repr
	/**
	  * @param p New color contrast standard pointer
	  * @return Copy of this context with the specified pointer used
	  */
	def withColorContrastStandardPointer(p: Changing[ColorContrastStandard]): Repr
	/**
	  * @param p New margins pointer
	  * @return Copy of this context with the specified pointer used
	  */
	def withMarginsPointer(p: Changing[Margins]): Repr
	/**
	  * @param p New stack margin pointer
	  * @return Copy of this context with the specified pointer used
	  */
	def withStackMarginPointer(p: Changing[StackLength]): Repr
	/**
	  * @param f A new flag which determines whether image-upscaling is allowed
	  * @return Copy of this context with the specified flag used
	  */
	def withAllowImageUpscalingFlag(f: Flag): Repr
	
	/**
	  * @param backgroundPointer A pointer to the applicable background color
	  * @return Copy of this context that adjusts itself against the specified background color
	  */
	def against(backgroundPointer: Changing[Color]): ColorSensitive
	
	
	// IMPLEMENTED  -------------------------
	
	override def font: Font = fontPointer.value
	override def colors: ColorScheme = colorsPointer.value
	override def contrastStandard: ColorContrastStandard = contrastStandardPointer.value
	override def margins: Margins = marginsPointer.value
	override def stackMargin: StackLength = stackMarginPointer.value
	override def smallStackMargin: StackLength = smallStackMarginPointer.value
	override def allowImageUpscaling: Boolean = allowImageUpscalingFlag.value
	
	override def withFont(font: Font): Repr = withFontPointer(Fixed(font))
	override def withColorContrastStandard(standard: ColorContrastStandard): Repr =
		withColorContrastStandardPointer(Fixed(standard))
	override def withMargins(margins: Margins): Repr = withMarginsPointer(Fixed(margins))
	override def withStackMargin(stackMargin: StackLength): Repr = withStackMarginPointer(Fixed(stackMargin))
	override def withAllowImageUpscaling(allowImageUpscaling: Boolean): Repr =
		withAllowImageUpscalingFlag(if (allowImageUpscaling) AlwaysTrue else AlwaysFalse)
		
	override def against(background: Color): ColorSensitive = against(Fixed(background))
	
	// TODO: May need termination condition for these maps (especially withStackMargin where merge is used)
	override def mapFont(f: Font => Font) = mapFontPointer { _.map(f) }
	override def mapMargins(f: Margins => Margins) = mapMarginsPointer { _.map(f) }
	override def mapStackMargin(f: StackLength => StackLength) = mapStackMarginPointer { _.map(f) }
	
	override def withStackMargin(size: SizeCategory) =
		withStackMarginPointer(marginsPointer.mergeWith(stackMarginPointer) { (margins, stackMargin) =>
			margins.scaleStackMargin(stackMargin, size)
		})
	
	override def against(color: ColorRole, shade: ColorLevel) =
		against(colorPointersCache(color -> shade))
		
	
	// OTHER    ----------------------------
	
	/**
	  * Modifies the font pointer used
	  * @param f A mapping function applied to the pointer
	  * @return Copy of this context with a modified pointer
	  */
	def mapFontPointer(f: Mutate[Changing[Font]]) = withFontPointer(f(fontPointer))
	/**
	  * Modifies the color scheme -pointer used
	  * @param f A mapping function applied to the pointer
	  * @return Copy of this context with a modified pointer
	  */
	def mapColorsPointer(f: Mutate[Changing[ColorScheme]]) = withColorsPointer(f(colorsPointer))
	/**
	  * Modifies the color-contrast standard -pointer used
	  * @param f A mapping function applied to the pointer
	  * @return Copy of this context with a modified pointer
	  */
	def mapContrastStandardPointer(f: Mutate[Changing[ColorContrastStandard]]) =
		withColorContrastStandardPointer(f(contrastStandardPointer))
	/**
	  * Modifies the margins pointer used
	  * @param f A mapping function applied to the pointer
	  * @return Copy of this context with a modified pointer
	  */
	def mapMarginsPointer(f: Mutate[Changing[Margins]]) = withMarginsPointer(f(marginsPointer))
	/**
	  * Modifies the stack margin pointer used
	  * @param f A mapping function applied to the pointer
	  * @return Copy of this context with a modified pointer
	  */
	def mapStackMarginPointer(f: Mutate[Changing[StackLength]]) = withStackMarginPointer(f(stackMarginPointer))
	/**
	  * Modifies the image-upscaling flag used
	  * @param f A mapping function applied to the flag
	  * @return Copy of this context with a modified flag
	  */
	def mapAllowImageUpscalingFlag(f: Mutate[Flag]) = withAllowImageUpscalingFlag(f(allowImageUpscalingFlag))
	
	/*
	  * @param color Targeted color role
	  * @param shade Applied color shade
	  * @return A pointer that contains the targeted color, according to the possibly varying color scheme
	  */
	// def colorPointer(color: ColorRole, shade: ColorLevel) = colorPointersCache(color -> shade)
}
