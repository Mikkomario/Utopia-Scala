package utopia.reflection.component.swing

import java.awt.Graphics

import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel._
import utopia.reflection.component.drawing.mutable.CustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer

/**
  * This trait is extended by awt components that allow custom drawing. Please note that this trait should only be
  * extended by actual awt component classes, not by wrappers. The extending class should override
  * paintComponent(Graphics), paintChildren(Graphics) and isPaintingOrigin()
  * @author Mikko Hilpinen
  * @since 29.4.2019, v1+
  */
trait CustomDrawComponent extends CustomDrawable
{
	// ATTRIBUTES	----------------------
	
	override var customDrawers = Vector[CustomDrawer]()
	
	
	// OTHER	--------------------------
	
	/**
	  * A custom paint component function. Should be used when overriding paintComponent
	  * @param g A graphics instance
	  * @param superPaintComponent The default implementation of paint component
	  */
	def customPaintComponent(g: Graphics, superPaintComponent: Graphics => Unit) = Drawer.use(g)
	{
		drawer =>
			customDraw(Background, drawer)
			superPaintComponent(drawer.graphics)
			customDraw(Normal, drawer)
	}
	
	/**
	  * A custom paint children function. Should be used when overriding paintChildren
	  * @param g A graphics instance
	  * @param superPaintChildren The default implementation of paint component
	  */
	def customPaintChildren(g: Graphics, superPaintChildren: Graphics => Unit) =
	{
		superPaintChildren(g)
		Drawer.use(g) { customDraw(Foreground, _) }
	}
	
	/**
	  * A custom implementation of isDrawingOrigin. Use when overriding said function.
	  * @return Whether this component should draw origin
	  */
	def shouldPaintOrigin() = customDrawers.exists { _.drawLevel == Foreground }
}
