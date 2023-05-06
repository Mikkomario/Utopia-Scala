package utopia.paradigm.enumeration

import utopia.paradigm.enumeration.Alignment.{Bottom, BottomLeft, BottomRight, Center, Top, TopLeft, TopRight}

/**
  * Common trait for classes that build items based on alignments
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.3.1
  */
trait FromAlignmentFactory[+A] extends FromDirectionFactory[A]
{
	// ABSTRACT --------------------
	
	/**
	  * @param alignment Targeted alignment
	  * @return An item for that alignment
	  */
	def apply(alignment: Alignment): A
	
	
	// COMPUTED --------------------
	
	def top = apply(Top)
	def bottom = apply(Bottom)
	def center = apply(Center)
	
	def topLeft = apply(TopLeft)
	def topRight = apply(TopRight)
	def bottomLeft = apply(BottomLeft)
	def bottomRight = apply(BottomRight)
	
	
	// IMPLEMENTED  ----------------
	
	override def left = apply(Alignment.Left)
	override def right = apply(Alignment.Right)
	
	override def apply(direction: Direction2D): A = apply(Alignment.forDirection(direction))
}
