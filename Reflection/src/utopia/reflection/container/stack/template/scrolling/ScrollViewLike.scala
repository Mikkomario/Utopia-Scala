package utopia.reflection.container.stack.template.scrolling

import utopia.paradigm.enumeration.Axis2D
import utopia.reflection.component.template.layout.stack.Stackable
import utopia.reflection.shape.stack.StackLengthLimit

import scala.collection.immutable.HashMap

/**
  * Scroll views are containers that allow horizontal or vertical content scrolling
  * @author Mikko Hilpinen
  * @since 30.4.2019, v1+
  */
@deprecated("Replaced with a new implementation", "v2.0")
trait ScrollViewLike[C <: Stackable] extends ScrollAreaLike[C]
{
	// ABSTRACT	--------------------
	
	/**
	  * @return The scrolling axis of this scroll view
	  */
	def axis: Axis2D
	/**
	  * @return The length limits of this scroll view
	  */
	def lengthLimit: StackLengthLimit
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return The length of this scroll view
	  */
	def length = lengthAlong(axis)
	def length_=(newLength: Double) = setLength(axis(newLength))
	/**
	  * @return The breadth of this scroll view
	  */
	def breadth = lengthAlong(axis.perpendicular)
	def breadth_=(newBreadth: Double) = setLength(axis.perpendicular(newBreadth))
	
	/**
	  * @return The length of this view's contents
	  */
	def contentLength = contentSize(axis)
	/**
	  * @return The breadth of this view's contents
	  */
	def contentBreadth = contentSize(axis.perpendicular)
	
	/**
	  * @return The current position of this view's contents (negative)
	  */
	def contentPosition = contentOrigin(axis)
	def contentPosition_=(pos: Double) = contentOrigin = contentOrigin.withDimension(axis(pos))
	
	/**
	  * @return The smallest possible content position (= position when scrolled at bottom)
	  */
	def minContentPosition = length - contentLength
	
	/**
	  * @return The current scroll modifier / percentage [0, 1]
	  */
	def scrollPercent = -contentPosition / contentLength
	def scrollPercent_=(newPercent: Double) = scrollTo(newPercent, axis, animated = false)
	
	/**
	  * @return Whether the content is currently scrolled to the top
	  */
	def isAtTop = contentPosition >= 0
	/**
	  * @return Whether the content is currently scrolled to the bottom
	  */
	def isAtBottom = contentPosition + contentLength <= length
	
	
	// IMPLEMENTED	----------------
	
	override def axes = Vector(axis)
	
	override def lengthLimits = HashMap(axis -> lengthLimit)
	
	
	// OTHER	----------------------
	
	/**
	  * Scrolls to a certain percentage
	  * @param abovePercent The percentage of content that should be above (outside) this view
	  * @param animated Whether scrolling should be animated (default = true)
	  */
	def scrollTo(abovePercent: Double, animated: Boolean): Unit = scrollTo(abovePercent, axis, animated)
	/**
	  * Scrolls to a certain percentage
	  * @param abovePercent The percentage of content that should be above (outside) this view
	  */
	def scrollTo(abovePercent: Double): Unit = scrollTo(abovePercent, animated = true)
	/**
	  * Scrolls this view a certain amount
	  * @param amount The amount of pixels scrolled
	  */
	def scroll(amount: Double) = contentPosition += amount
}
