package utopia.genesis.util

import java.awt.{AlphaComposite, BasicStroke, Font, Graphics, Graphics2D, Image, Paint, Shape, Stroke, Toolkit}
import java.awt.geom.AffineTransform

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.{Bounds, Point, ShapeConvertible, Size, Transformation}
import utopia.flow.util.NullSafe._

import scala.util.Try

object Drawer
{
    /**
     * Creates a stroke with default settings
     */
    def defaultStroke(width: Double) = new BasicStroke(width.toFloat)
    /**
     * Creates a rounded stroke instance
     */
    def roundStroke(width: Double) = new BasicStroke(width.toFloat, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    
    /**
      * Creates a new temporary drawer for the duration of the operation by wrapping an awt graphics instance.
      * Disposes the drawer afterwards.
      * @param graphics A graphics instance
      * @param f A function that uses the drawer
      */
    def use(graphics: Graphics)(f: Drawer => Unit) = new Drawer(graphics.create().asInstanceOf[Graphics2D]).disposeAfter(f)
}

/**
 * A drawer instance uses a graphics instance to draw elements. Different transformations and
 * settings can be applied to a drawer. The usual policy is to not modify the drawers directly but
 * always make new copies instead. The underlying graphics object should therefore only be altered
 * when this class doesn't provide a suitable interface to do that otherwise. Because a mutable
 * graphics object instance is made visible, this class does not have value semantics and should be
 * treated with care. It is usually better to pass on a copy of this object when its usage cannot be
 * controlled.
 * @author Mikko Hilpinen
 * @since 22.1.2017
 */
class Drawer(val graphics: Graphics2D, val fillPaint: Option[Paint] = Some(java.awt.Color.WHITE),
             val edgePaint: Option[Paint] = Some(java.awt.Color.BLACK))
{
    // TODO: Add rendering hints
    
    // ATTRIBUTES    ----------------------
    
    private var children = Vector[Drawer]()
    
    
    // COMPUTED ---------------------------
    
    /**
      * @return A version of this drawer that only fills shapes. May return this drawer.
      */
    def onlyFill = if (edgePaint.isDefined) copy(fillPaint, None) else this
    
    /**
      * @return A version of this drawer that only draws edges of shapes. May return this drawer.
      */
    def onlyEdges = if (fillPaint.isDefined) copy(None, edgePaint) else this
    
    /**
      * @return A version of this drawer that only draws edges of shapes. May return this drawer.
      */
    def noFill = onlyEdges
    
    /**
      * @return A version of this drawer that only fills shapes. May return this drawer.
      */
    def noEdges = onlyFill
    
    
    // OTHER METHODS    -------------------
    
    /**
      * Performs an operation, then disposes this drawer
      * @param operation An operation
      * @tparam U Arbitary result type
      */
    def disposeAfter[U](operation: Drawer => U) =
    {
        operation(this)
        dispose()
    }
    
    /**
      * Creates a temporary copy of this drawer and performs an operation with it, after which it's disposed
      * @param operation Operation performed with the drawer
      * @tparam U Arbitary result type
      */
    def withCopy[U](operation: Drawer => U) = copy().disposeAfter(operation)
    
    /**
     * Copies this drawer, creating another graphics context. Changing the other context doesn't
     * affect this one. This should be used when a lot of drawing is done and the graphics context
     * should be returned to its original state.
     */
    def copy(): Drawer = copy(fillPaint, edgePaint)
    
    /**
     * Disposes this graphics context and every single drawer created from this drawer instance. The
     * responsibility to dispose the drawer lies at the topmost user. Disposing drawers on lower
     * levels is fully optional. This drawer or any drawer created from this drawer cannot be used
     * after it has been disposed.
     */
    def dispose(): Unit =
    {
        children.foreach { _.dispose() }
        graphics.dispose()
    }
    
    /**
     * Draws and fills a shape
     */
    def draw(shape: Shape) =
    {
        if (fillPaint.isDefined)
        {
            graphics.setPaint(fillPaint.get)
            graphics.fill(shape)
        }
        if (edgePaint.isDefined)
        {
            graphics.setPaint(edgePaint.get)
            graphics.draw(shape)
        }
    }
    
    /**
     * Draws a shape convertible instance as this drawer would draw a shape
     */
    def draw(shape: ShapeConvertible): Unit = draw(shape.toShape)
    
    /**
     * Draws a piece of text so that it is centered in a set of bounds
     */
    def drawTextCentered(text: String, font: Font, bounds: Bounds) =
    {
        drawTextPositioned(text, font) { textSize => bounds.topLeft + (bounds.size - textSize) / 2 }
    }
    
    /**
      * Draws a piece of text
      * @param text The text that is drawn
      * @param font Font used
      * @param topLeft The top left position of the text
      */
    def drawText(text: String, font: Font, topLeft: Point) =
    {
        // Sets the color, preferring edge color
        prepareForTextDraw(font)
        val metrics = graphics.getFontMetrics
        graphics.drawString(text, topLeft.x.toInt, topLeft.y.toInt + metrics.getAscent)
    }
    
    /**
      * Draws a piece of text. Text display size affects the positioning
      * @param text Text to draw
      * @param font Font to use
      * @param getTextTopLeft A function for determining the position of the <b>top-left</b> corner of the drawn
      *                       text when text size is known.
      */
        // TODO: Handle multiline text
    def drawTextPositioned(text: String, font: Font)(getTextTopLeft: Size => Point) =
    {
        // Sets up the graphics context
        prepareForTextDraw(font)
        val metrics = graphics.getFontMetrics
        
        val textSize = Size(metrics.stringWidth(text), metrics.getHeight)
        val textTopLeft = getTextTopLeft(textSize)
        
        graphics.drawString(text, textTopLeft.x.toInt, textTopLeft.y.toInt + metrics.getAscent)
    }
    
    /**
      * Draws an image
      * @param image The image to be drawn
      * @param position The position where the origin is drawn
      * @param origin The relative position of the image's origin, the point which is placed at (0, 0) coordinates (default = (0, 0))
      * @return Whether the image was fully loaded and drawn
      */
    def drawImage(image: utopia.genesis.image.Image, position: Point, origin: Point): Boolean = image.drawWith(this, position, origin)
    
    /**
      * Draws an image
      * @param image The image to be drawn
      * @param position The position of the image's top left corner (default = (0, 0))
      * @return Whether the image was fully loaded and drawn
      */
    def drawImage(image: Image, position: Point = Point.origin): Boolean =
        graphics.drawImage(image, position.x.toInt, position.y.toInt, null)
    
    /**
     * Creates a transformed copy of this
     * drawer so that it will use the provided transformation to draw relative
     * elements into absolute space
     */
    def transformed(transform: AffineTransform) =
    {
        val drawer = copy()
        drawer.graphics.transform(transform)
        drawer
    }
    
    /**
     * Creates a transformed copy of this
     * drawer so that it will use the provided transformation to draw relative
     * elements into absolute space
     */
    def transformed(transform: Transformation): Drawer = transformed(transform.toAffineTransform)
    
    /*
     * Creates a transformed copy of this drawer so that it reads from absolute world space and
     * projects them differently on another absolute world space
     * @param from The transformation with which the data is read
     * @param to The transformation with which the data is projected (like in other transform
     * methods)
     */
    //def transformed(from: Transformation, to: Transformation): Drawer =
    //        transformed(from.toInvertedAffineTransform).transformed(to);
    
    /**
     * Creates a new instance of this drawer with altered colours
     * @param fill the colour / paint used for filling the area
     * @param edge the colour / paint used for the drawn edges. By default stays the same as it
     * was in the original
     */
    def withPaint(fill: Option[Paint], edge: Option[Paint] = this.edgePaint) = copy(fill, edge)
    
    /**
      * Creates a new instance of this drawer with altered colours
      * @param fill the colour / paint used for filling the area
      * @param edge the colour / paint used for the drawn edges
      */
    def withPaint(fill: Paint, edge: Paint): Drawer = withPaint(Some(fill), Some(edge))
    
    /**
      * Creates a new instance of this drawer with altered colors
      * @param fill the color used for filling the area
      * @param edge the color used for the drawn edges
      */
    def withColor(fill: Option[Color], edge: Option[Color]) = withPaint(fill.map { _.toAwt }, edge.map { _.toAwt })
    
    /**
      * Creates a new instance of this drawer with altered colors
      * @param fill the color used for filling the area
      * @param edge the color used for the drawn edges
      */
    def withColor(fill: Color, edge: Color): Drawer = withPaint(fill.toAwt, edge.toAwt)
    
    /**
     * Creates a new instance of this drawer with altered edge colour
     */
    def withEdgePaint(edge: Option[Paint]) = withPaint(fillPaint, edge)
    
    /**
      * @param color The new edge drawing color
      * @return A version of this drawer with specified edge color
      */
    def withEdgePaint(color: Paint): Drawer = withEdgePaint(Some(color))
    
    /**
      * Creates a new instance of this drawer with altered edge color
      * @param edge The new edge color
      * @return A copy of this drawer with altered color
      */
    def withEdgeColor(edge: Option[Color]) = withEdgePaint(edge.map { _.toAwt })
    
    /**
      * @param color The new edge drawing color
      * @return A version of this drawer with specified edge color
      */
    def withEdgeColor(color: Color): Drawer = withEdgePaint(color.toAwt)
    
    /**
      * Creates a new drawer with altered fill color
      * @param fill The new fill color
      * @return A copy of this drawer with new fill color
      */
    def withFillPaint(fill: Option[Paint]) = withPaint(fill, edgePaint)
    
    /**
      * @param color The new fill color
      * @return A version of this drawer with specified fill color
      */
    def withFillPaint(color: Paint): Drawer = withFillPaint(Some(color))
    
    /**
      * @param color The new fill color
      * @return A version of this drawer with specified fill color
      */
    def withFillColor(color: Color) = withFillPaint(Some(color.toAwt))
    
    /**
      * @param color The new edge drawing color
      * @return A copy of this drawer with specified edge color and no fill
      */
    def onlyEdges(color: Paint) = withPaint(None, Some(color))
    
    /**
      * @param color The new edge drawing color
      * @return A copy of this drawer with specified edge color and no fill
      */
    def onlyEdges(color: Color): Drawer = onlyEdges(color.toAwt)
    
    /**
      * @param color The new fill color
      * @return A copy of this drawer with specified fill color and no edges
      */
    def onlyFill(color: Paint) = withPaint(Some(color), None)
    
    /**
      * @param color The new fill color
      * @return A copy of this drawer with specified fill color and no edges
      */
    def onlyFill(color: Color): Drawer = onlyFill(color.toAwt)
    
    /**
     * Creates a copy of this context with altered alpha (opacity / transparency) value.
     * @param alpha Between 0 and 1. 1 Means that the drawn elements are fully visible / not
     * transparent while lower numbers increase the transparency.
     */
    def withAlpha(alpha: Double) = 
    {
        val drawer = copy()
        drawer.graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.toFloat))
        drawer
    }
    
    /**
     * Copies this graphics context, changing the stroke style in the process. It is usually better
     * to pass a drawer instance with unmodified stroke where that is possible, since it is better
     * optimised in the lower level implementation.
     */
    def withStroke(stroke: Stroke) = 
    {
        val drawer = copy()
        drawer.graphics.setStroke(stroke)
        drawer
    }
    
    /**
      * Copies this graphics context, changing the stroke style in the process. It is usually better
      * to pass a drawer instance with unmodified stroke where that is possible, since it is better
      * optimised in the lower level implementation.
      * @param strokeWidth The width of the new stroke
      */
    def withStroke(strokeWidth: Int): Drawer = withStroke(Drawer.defaultStroke(strokeWidth))
    
    /**
     * Creates a new instance of this drawer that has a clipped drawing area. The operation cannot
     * be reversed but the original instance can still be used for drawing without clipping.
     */
    def clippedTo(shape: Shape) = 
    {
        val drawer = copy()
        drawer.graphics.clip(shape)
        drawer
    }
    
    /**
     * Creates a new instance of this drawer that has a clipped drawing area. The operation cannot
     * be reversed but the original instance can still be used for drawing without clipping.
     */
    def clippedTo(shape: ShapeConvertible): Drawer = clippedTo(shape.toShape)
    
    private def prepareForTextDraw(font: Font) =
    {
        edgePaint.orElse(fillPaint).foreach(graphics.setPaint)
        graphics.setFont(font)
        
        // Sets rendering hints based on desktop settings
        Try { Toolkit.getDefaultToolkit.getDesktopProperty("awt.font.desktophints").toOption
            .map { _.asInstanceOf[java.util.Map[_, _]] }.foreach(graphics.setRenderingHints) }
    }
    
    private def copy(fillColor: Option[Paint], edgeColor: Option[Paint]) = 
    {
        val drawer = new Drawer(graphics.create().asInstanceOf[Graphics2D], fillColor, edgeColor)
        children :+= drawer
        
        drawer
    }
}