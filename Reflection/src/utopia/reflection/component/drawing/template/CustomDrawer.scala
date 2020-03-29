package utopia.reflection.component.drawing.template

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal

import scala.language.implicitConversions

object CustomDrawer
{
	/**
	  * Wraps a function into custom drawer
	  * @param level The target draw level (default = Normal = Above component contents but below child contents)
	  * @param f A function
	  * @return A new custom drawer that calls that function
	  */
	def apply(level: DrawLevel = Normal)(f: (Drawer, Bounds) => Unit): CustomDrawer = new FunctionalCustomDrawer(level, f)
}

/**
  * Custom drawers perform custom drawer atop normal component drawing
  * @author Mikko Hilpinen
  * @since 29.4.2019, v1+
  */
trait CustomDrawer
{
	/**
	  * @return The level where this drawer will be drawn
	  */
	def drawLevel: DrawLevel
	
	/**
	  * Performs the drawing
	  * @param drawer A drawer used for the drawing (origin located at parent component origin)
	  * @param bounds Draw area bounds
	  */
	def draw(drawer: Drawer, bounds: Bounds): Unit
}

private class FunctionalCustomDrawer(override val drawLevel: DrawLevel, val f: (Drawer, Bounds) => Unit) extends CustomDrawer
{
	override def draw(drawer: Drawer, bounds: Bounds) = f(drawer, bounds)
}