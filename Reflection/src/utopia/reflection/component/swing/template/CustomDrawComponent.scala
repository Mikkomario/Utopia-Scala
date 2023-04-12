package utopia.reflection.component.swing.template

import utopia.firmament.drawing.mutable.MutableCustomDrawable
import utopia.genesis.graphics.Drawer
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.{Background, Foreground, Normal}

import java.awt.{Graphics, Graphics2D}

/**
  * This trait is extended by awt components that allow custom drawing. Please note that this trait should only be
  * extended by actual awt component classes, not by wrappers. The extending class should override
  * paintComponent(Graphics), paintChildren(Graphics) and isPaintingOrigin()
  * @author Mikko Hilpinen
  * @since 29.4.2019, v1+
  */
trait CustomDrawComponent extends MutableCustomDrawable
{
	// ATTRIBUTES	----------------------
	
	override var customDrawers = Vector[CustomDrawer]()
	
	
	// OTHER	--------------------------
	
	/**
	  * A custom paint component function. Should be used when overriding paintComponent
	  * @param g A graphics instance
	  * @param superPaintComponent The default implementation of paint component
	  */
	def customPaintComponent(g: Graphics, superPaintComponent: Graphics => Unit) = {
		val graphics = g.asInstanceOf[Graphics2D]
		Drawer(graphics).use { drawer =>
			customDraw(Background, drawer)
			superPaintComponent(graphics)
			customDraw(Normal, drawer)
		}
	}
	
	/**
	  * A custom paint children function. Should be used when overriding paintChildren
	  * @param g A graphics instance
	  * @param superPaintChildren The default implementation of paint component
	  */
	def customPaintChildren(g: Graphics, superPaintChildren: Graphics => Unit) = {
		superPaintChildren(g)
		Drawer(g.asInstanceOf[Graphics2D]).use { customDraw(Foreground, _) }
	}
	
	/**
	  * A custom implementation of isDrawingOrigin. Use when overriding said function.
	  * @return Whether this component should draw origin
	  */
	def shouldPaintOrigin() = customDrawers.exists { _.drawLevel == Foreground }
}
