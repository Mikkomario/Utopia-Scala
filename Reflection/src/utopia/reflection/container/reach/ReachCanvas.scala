package utopia.reflection.container.reach

import java.awt.{Container, Graphics}

import javax.swing.{JComponent, JPanel}
import utopia.flow.async.VolatileOption
import utopia.flow.async.AsyncExtensions._
import utopia.genesis.shape.shape2D.{Bounds, Point}
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.mutable.CustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.{Background, Foreground, Normal}
import utopia.reflection.component.reach.template.ReachComponentLike
import utopia.reflection.component.swing.template.{JWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.{StackLeaf, Stackable}
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.shape.stack.StackSize

import scala.concurrent.Future

/**
  * The component that connects a reach component hierarchy to the swing component hierarchy
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
class ReachCanvas(contentFuture: Future[ReachComponentLike]) extends JWrapper with Stackable
	with AwtContainerRelated with SwingComponentRelated with CustomDrawable with StackLeaf
{
	// ATTRIBUTES	---------------------------
	
	override var customDrawers = Vector[CustomDrawer]()
	
	private val panel = new CustomDrawPanel()
	private val repaintNeed = VolatileOption[RepaintNeed](Full)
	
	
	// INITIAL CODE	---------------------------
	
	// Requires a full repaint after attached to the stack hierarchy
	addStackHierarchyAttachmentListener { repaintNeed.setOne(Full) }
	// Also requires a full repaint when size changes
	addResizeListener { event =>
		if (event.newSize.isPositive)
			repaintNeed.setOne(Full)
	}
	
	
	// COMPUTED	-------------------------------
	
	private def currentContent = contentFuture.current.flatMap { _.toOption }
	
	
	// IMPLEMENTED	---------------------------
	
	override def component: JComponent with Container = panel
	
	override def updateLayout() = currentContent.foreach { _.updateLayout() }
	
	override def stackSize = currentContent match
	{
		case Some(content) => content.stackSize
		case None => StackSize.any
	}
	
	override def resetCachedSize() = currentContent.foreach { _.resetCachedSize() }
	
	override val stackId = hashCode()
	
	override def drawBounds = Bounds(Point.origin, size)
	
	override def repaint() =
	{
		repaintNeed.setOne(Full)
		component.repaint()
	}
	
	
	// OTHER	------------------------------
	
	/**
	  * Repaints a part of this canvas
	  * @param area Area to paint again
	  */
	def repaint(area: Bounds) =
	{
		repaintNeed.update
		{
			case Some(old) =>
				old match
				{
					case Full => Some(old)
					case Partial(oldArea) => Some(Partial(Bounds.around(Vector(oldArea, area))))
				}
			case None => Some(Partial(area))
		}
	}
	
	
	// NESTED	------------------------------
	
	private class CustomDrawPanel extends JPanel(null)
	{
		// INITIAL CODE	---------------------
		
		setOpaque(false)
		
		
		// IMPLEMENTED	----------------------
		
		override def paintComponent(g: Graphics) = Drawer.use(g) { drawer =>
			// Only repaints if necessary
			repaintNeed.pop().foreach { need =>
				// Determines draw area
				val area = need match
				{
					case Full => None
					case Partial(area) => Some(area)
				}
				// Draws background, if defined
				lazy val fullDrawBounds = drawBounds
				if (isOpaque)
					drawer.onlyFill(background).draw(area.getOrElse(fullDrawBounds))
				val drawersPerLayer = customDrawers.groupBy { _.drawLevel }
				// Draws background custom drawers and then normal custom drawers, if defined
				val backgroundAndNormalDrawers = drawersPerLayer.getOrElse(Background, Vector()) ++
					drawersPerLayer.getOrElse(Normal, Vector())
				if (backgroundAndNormalDrawers.nonEmpty)
				{
					val d = area.map(drawer.clippedTo).getOrElse(drawer)
					backgroundAndNormalDrawers.foreach { _.draw(d, fullDrawBounds) }
				}
				// Draws component content
				currentContent.foreach { _.paintWith(drawer, area) }
				// Draws foreground, if defined
				drawersPerLayer.get(Foreground).foreach { drawers =>
					val d = area.map(drawer.clippedTo).getOrElse(drawer)
					drawers.foreach { _.draw(d, fullDrawBounds) }
				}
			}
		}
		
		// Never paints children (because won't have any children)
		override def paintChildren(g: Graphics) = ()
	}
	
	private sealed trait RepaintNeed
	
	private case object Full extends RepaintNeed
	
	private case class Partial(area: Bounds) extends RepaintNeed
}
