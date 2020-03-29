package utopia.reflection.component

import utopia.reflection.shape.Alignment
import utopia.reflection.shape.Alignment._

/**
  * Alignable elements can be aligned
  * @author Mikko Hilpinen
  * @since 22.4.2019, v1+
  */
trait Alignable
{
	// ABSTRACT	----------------
	
	/**
	  * Aligns the contents of this component
	  * @param alignment The target alignment
	  */
	def align(alignment: Alignment): Unit
	
	
	// OTHER	---------------
	
	/**
	  * Aligns the contents of this component to the left
	  */
	def alignLeft() = align(Left)
	
	/**
	  * Aligns the contents of this component to the right
	  */
	def alignRight() = align(Right)
	
	/**
	  * Aligns the contents of this component to the center
	  */
	def alignCenter() = align(Center)
	
	/**
	  * Aligns the contents of this component to the top
	  */
	def alignTop() = align(Top)
	
	/**
	  * Aligns the contents of this component to the bottom
	  */
	def alignBottom() = align(Bottom)
}
