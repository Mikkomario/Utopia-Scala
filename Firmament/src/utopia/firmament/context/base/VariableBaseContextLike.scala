package utopia.firmament.context.base

import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.StackLength
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.text.Font
import utopia.paradigm.color.Color

/**
  * Common trait for pointer-based context implementations that provide access to the basic context properties
  * @author Mikko Hilpinen
  * @since 27.09.2024, v1.3.2
  */
trait VariableBaseContextLike[+Repr, +ColorSensitive]
	extends BaseContextPropsView with BaseContextCopyable[Repr, ColorSensitive]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @param p New font pointer
	  * @return Copy of this context with the specified font pointer used
	  */
	def withFontPointer(p: Changing[Font]): Repr
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
	
	override def withFont(font: Font): Repr = withFontPointer(Fixed(font))
	override def withStackMargin(stackMargin: StackLength): Repr = withStackMarginPointer(Fixed(stackMargin))
	override def withAllowImageUpscaling(allowImageUpscaling: Boolean): Repr =
		withAllowImageUpscalingFlag(if (allowImageUpscaling) AlwaysTrue else AlwaysFalse)
		
	override def against(background: Color): ColorSensitive = against(Fixed(background))
	
	// TODO: May need termination condition for these maps
	override def mapFont(f: Font => Font) = mapFontPointer { _.map(f) }
	override def mapStackMargin(f: StackLength => StackLength) = mapStackMarginPointer { _.map(f) }
	
	override def withStackMargin(size: SizeCategory) =
		mapStackMarginPointer { _.map { margins.scaleStackMargin(_, size) } }
		
	
	// OTHER    ----------------------------
	
	/**
	  * Modifies the font pointer used
	  * @param f A mapping function applied to the pointer
	  * @return Copy of this context with a modified pointer
	  */
	def mapFontPointer(f: Mutate[Changing[Font]]) = withFontPointer(f(fontPointer))
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
}
