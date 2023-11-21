package utopia.paradigm.color

import utopia.paradigm.color.ColorShade.{Dark, Light}

/**
  * Common trait for items that can return a light or a dark variant
  * @author Mikko Hilpinen
  * @since 10.4.2023, v1.2.1
  */
trait FromShadeFactory[+A]
{
	// ABSTRACT -----------------------
	
	/**
	  * @param shade Targeted shade
	  * @return An item matching that shade
	  */
	def apply(shade: ColorShade): A
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return A light version of this item
	  */
	def light = apply(Light)
	/**
	  * @return A dark version of this item
	  */
	def dark = apply(Dark)
	
	
	// OTHER    ---------------------
	
	/**
	  * @param shade Shade of the background
	  * @return A version of this item that is most appropriate against that shade
	  */
	def against(shade: ColorShade) = apply(shade.opposite)
	/**
	  * @param color A color
	  * @return An item that is best visible against that color
	  */
	def against(color: Color): A = against(color.shade)
}
