package utopia.genesis.graphics

import utopia.genesis.graphics.DrawLevel2.{Background, Foreground}

/**
  * Common trait for factory classes that produce items based on draw-levels
  * @author Mikko Hilpinen
  * @since 23/02/2024, v4.0
  */
trait FromDrawLevelFactory[+A]
{
	// ABSTRACT -------------------------
	
	/**
	  * @param drawLevel Targeted draw level
	  * @return An item with/at that draw level
	  */
	def apply(drawLevel: DrawLevel2): A
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return An item at the foreground
	  */
	def foreground = apply(Foreground)
	/**
	  * @return An item at the background
	  */
	def background = apply(Background)
}
