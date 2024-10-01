package utopia.firmament.context.base

import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.StackLength
import utopia.flow.view.immutable.eventful.{Always, Fixed}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.text.Font

/**
  * A trait common for basic component context implementations
  * @author Mikko Hilpinen
  * @since 29.9.2024, v1.3.1
  * @tparam Repr This context type
  * @tparam ColorSensitive A color-sensitive version of this context
  */
trait StaticBaseContextLike[+Repr, +ColorSensitive] extends BaseContextCopyable[Repr, ColorSensitive]
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Used font
	  */
	def font: Font
	
	/**
	  * @return The default margin placed between items in a stack
	  */
	def stackMargin: StackLength
	/**
	  * @return The margin placed between items in a stack when they are more closely related
	  */
	def smallStackMargin: StackLength
	
	/**
	  * @return Whether images and icons should be allowed to scale above their original resolution. When this is
	  *         enabled, images will fill the desired screen space, but they will be blurry.
	  */
	def allowImageUpscaling: Boolean
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Smallest allowed (stack) margin
	  */
	def minMargin = stackMargin.min
	/**
	  * @return Optimal (stack) margin
	  */
	def optimalMargin = stackMargin.optimal
	/**
	  * @return Largest allowed (stack) margin
	  */
	def maxMargin = stackMargin.max
	
	
	// IMPLEMENTED  ---------------------
	
	override def fontPointer: Changing[Font] = Fixed(font)
	override def stackMarginPointer: Changing[StackLength] = Fixed(stackMargin)
	override def smallStackMarginPointer: Changing[StackLength] = Fixed(smallStackMargin)
	override def allowImageUpscalingFlag: Flag = Always(allowImageUpscaling)
	
	/**
	  * @param size New stack margins size category to use
	  * @return A copy of this context with those stack margins in use
	  */
	def withStackMargin(size: SizeCategory): Repr = withStackMargin(scaledStackMargin(size))
	
	/**
	  * @param f A mapping function for the font used
	  * @return A mapped copy of this context
	  */
	def mapFont(f: Font => Font) = withFont(f(font))
	/**
	  * @param f A mapping function for stack margins to use
	  * @return A mapped copy of this context
	  */
	def mapStackMargin(f: StackLength => StackLength) = withStackMargin(f(stackMargin))
	
	override def scaledStackMarginPointer(scaling: SizeCategory): Changing[StackLength] =
		Fixed(scaledStackMargin(scaling))
	
	
	// OTHER    -------------------------
	
	/**
	  * @param scaling Targeted stack margin size
	  * @return Stack margin of this context, scaled to the specified size
	  */
	def scaledStackMargin(scaling: SizeCategory) = margins.scaleStackMargin(stackMargin, scaling)
}
