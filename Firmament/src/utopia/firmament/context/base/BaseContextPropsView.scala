package utopia.firmament.context.base

import utopia.firmament.localization.Localizer
import utopia.firmament.model.Margins
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.StackLength
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorScheme
import utopia.paradigm.enumeration.ColorContrastStandard

/**
  * Common trait for context instances that provide view access to basic context properties
  * @author Mikko Hilpinen
  * @since 29.9.2024
  */
trait BaseContextPropsView extends Any
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Actor handler used for distributing action events
	  */
	def actorHandler: ActorHandler
	/**
	  * @return A localizer used in this context
	  */
	def localizer: Localizer
	
	/**
	  * @return The color scheme to be used
	  */
	def colors: ColorScheme
	/**
	  * @return Color contrast standard being applied
	  */
	def contrastStandard: ColorContrastStandard
	/**
	  * @return Used margins
	  */
	def margins: Margins
	
	/**
	  * @return A pointer that contains the currently used font
	  */
	def fontPointer: Changing[Font]
	/**
	  * @return A pointer that contains the default margin placed between items in a stack
	  */
	def stackMarginPointer: Changing[StackLength]
	/**
	  * @return The margin placed between items in a stack when they are more closely related
	  */
	def smallStackMarginPointer: Changing[StackLength]
	
	/**
	  * @return A flag that determines
	  *         whether images and icons should be allowed to scale above their original resolution.
	  *         When enabled, images will fill the desired screen space, but they may be blurry.
	  */
	def allowImageUpscalingFlag: Flag
	
	/**
	  * @param scaling Targeted stack margin size
	  * @return Stack margin of this context, scaled to the specified size
	  */
	def scaledStackMarginPointer(scaling: SizeCategory): Changing[StackLength]
	/**
	  * @param scalingPointer A pointer that contains the targeted stack margin size
	  * @return Stack margin of this context, scaled to the specified (variable) size
	  */
	def scaledStackMarginPointer(scalingPointer: Changing[SizeCategory]): Changing[StackLength]
	/**
	  * @param smallFlag A pointer that contains true when small stack margin should be used and false when
	  *                  the default stack margin should be used.
	  * @return A pointer that contains either the small or the default stack margin, matching the state of 'smallFlag'.
	  */
	def stackMarginPointerFor(smallFlag: Flag): Changing[StackLength]
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Button border width to use by default
	  */
	def buttonBorderWidth = (margins.verySmall / 2.0).round.toInt
	
	
	// OTHER    -------------------------
	
	/**
	  * @param small True if targeting the small stack margin. False otherwise.
	  * @return Stack margin pointer that matches the specified size scale.
	  */
	def stackMarginPointerFor(small: Boolean) =
		if (small) smallStackMarginPointer else stackMarginPointer
}
