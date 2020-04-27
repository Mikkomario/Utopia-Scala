package utopia.reflection.component.context

import utopia.genesis.handling.mutable.ActorHandler
import utopia.reflection.color.ColorScheme
import utopia.reflection.shape.Margins
import utopia.reflection.text.Font

/**
  * A trait common for basic component context implementations
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
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
}
