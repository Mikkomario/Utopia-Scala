package utopia.firmament.drawing.mutable

import utopia.reflection.component.drawing.template.CustomDrawer

/**
  * A common trait for classes which wrap a mutable custom drawable and want to expose it
  * @author Mikko Hilpinen
  * @since 25.10.2020, Reflection v2
  */
trait MutableCustomDrawableWrapper extends MutableCustomDrawable
{
	// ABSTRACT	---------------------------
	
	/**
	  * @return The wrapped component
	  */
	protected def drawable: MutableCustomDrawable
	
	
	// IMPLEMENTED	------------------------
	
	override def customDrawers_=(drawers: Vector[CustomDrawer]) = drawable.customDrawers = drawers
	
	override def customDrawers = drawable.customDrawers
	
	override def drawBounds = drawable.drawBounds
	
	override def repaint() = drawable.repaint()
}
