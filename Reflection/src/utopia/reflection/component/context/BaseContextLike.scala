package utopia.reflection.component.context

import utopia.firmament.model.Margins
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.text.Font
import utopia.reflection.color.ColorScheme
import utopia.reflection.shape.stack.StackLength

/**
  * A trait common for basic component context implementations
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
@deprecated("Moved to Firmament", "v2.0")
trait BaseContextLike
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Actor handler used for distributing action events
	  */
	def actorHandler: ActorHandler
	
	/**
	  * @return Used font
	  */
	def defaultFont: Font
	
	/**
	  * @return The color scheme used in the program by default
	  */
	def defaultColorScheme: ColorScheme
	
	/**
	  * @return Used margins
	  */
	def margins: Margins
	
	/**
	  * @return The default margin placed between items in a stack
	  */
	def defaultStackMargin: StackLength
	
	/**
	  * @return The margin placed between items in a stack when they are more closely related
	  */
	def relatedItemsStackMargin: StackLength
	
	/**
	  * @return Whether images and icons should be allowed to scale above their original resolution. When this is
	  *         enabled, images will fill the desired screen space but they will be blurry.
	  */
	def allowImageUpscaling: Boolean
}
