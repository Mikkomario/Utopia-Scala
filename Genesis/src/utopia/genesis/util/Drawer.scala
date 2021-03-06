package utopia.genesis.util

import java.awt.{AlphaComposite, BasicStroke, Font, Graphics, Graphics2D, Image, Paint, RenderingHints, Shape, Stroke, Toolkit}
import java.awt.geom.AffineTransform
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.{Bounds, JavaAffineTransformConvertible, Matrix2D, Point, ShapeConvertible, Size, TwoDimensional}
import utopia.flow.util.NullSafe._
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.shape2D.transform.{AffineTransformable, AffineTransformation, LinearTransformable}
import utopia.genesis.shape.shape3D.{Matrix3D, Vector3D}

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
    extends LinearTransformable[Drawer] with AffineTransformable[Drawer]
{
    // TODO: Add rendering hints
    
    // ATTRIBUTES    ----------------------
    
    /**
      * The current clipping bounds assigned to this drawer (None if no clipping bounds have been assigned)
      */
    lazy val clipBounds = Option(graphics.getClipBounds).map { r => r: Bounds }
    
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
    
    
    // IMPLEMENTED  -----------------------
    
    override def transformedWith(transformation: Matrix2D) = transformed(transformation)
    
    override def transformedWith(transformation: Matrix3D) = transformed(transformation)
    
    
    // OTHER METHODS    -------------------
    
    /**
     * @param font A font
     * @return Font metrics to use with that font
     */
    def fontMetricsWith(font: Font) = graphics.getFontMetrics(font)
    
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
        fillPaint.foreach { fill =>
            graphics.setPaint(fill)
            graphics.fill(shape)
        }
        edgePaint.foreach { edge =>
            graphics.setPaint(edge)
            graphics.draw(shape)
        }
    }
    
    /**
     * Draws a shape convertible instance as this drawer would draw a shape
     */
    def draw(shape: ShapeConvertible): Unit = draw(shape.toShape)
    
    /**
      * Draws a piece of text so that it is centered in a set of bounds
      * @param text The text being drawn
      * @param font Font used when drawing the text
      * @param bounds Bounds within which the text must fit
      * @param betweenLinesMargin Margin between lines of text (used if there are multiple lines, default = 0)
      * @param allowMultipleLines Whether use of multiple text lines should be allowed (default = true)
      */
    def drawTextCentered(text: String, font: Font, bounds: Bounds, betweenLinesMargin: Double = 0.0,
                         allowMultipleLines: Boolean = true) =
    {
        drawTextPositioned(text, font, betweenLinesMargin, (lw, tw) => (tw - lw) / 2.0, allowMultipleLines) { textSize =>
            // May downscale the text to fit the bounds
            val scaling = (bounds.size / textSize).minDimension min 1
            val scaledSize = textSize * scaling
            val topLeft = bounds.topLeft + (bounds.size - scaledSize) / 2
            Bounds(topLeft, scaledSize)
        }
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
        graphics.drawString(text, topLeft.x.round.toInt, topLeft.y.round.toInt + metrics.getAscent)
    }
    
    /**
      * Draws a piece of text, expecting font to be already set
      * @param text Text to draw
      * @param topLeft The top left position of the text
      */
    def drawTextRaw(text: String, topLeft: Point) = graphics.drawString(text, topLeft.x.round.toInt,
        topLeft.y.round.toInt + graphics.getFontMetrics.getAscent)
    
    /**
      * Draws a piece of text. Specified bounds affect the positioning and possibly scaling.
      * @param text Text to draw
      * @param font Font to use
      * @param betweenLinesMargin Margin placed between lines of text if multiple lines are used (default = 0.0)
      * @param lineX A function for calculating the left x-coordinate of a text line based on the line width and the
      *              maximum line width. Default = all lines start at the same position (0)
      * @param allowMultipleLines Whether use of multiple text lines should be allowed (default = true)
      * @param getTextArea A function for determining the bounds of the drawn text when text size is known
      */
    def drawTextPositioned(text: String, font: Font, betweenLinesMargin: Double = 0.0,
                           lineX: (Double, Double) => Double = (_, _) => 0.0, allowMultipleLines: Boolean = true)
                          (getTextArea: Size => Bounds) =
    {
        if (allowMultipleLines)
            drawMultilineTextPositioned(text, font, betweenLinesMargin)(getTextArea)(lineX)
        else
            drawSingleLineTextPositioned(text, font)(getTextArea)
    }
    
    /**
      * Draws a piece of text as a single horizontal line. Specified bounds affect the positioning and possibly scaling.
      * @param text Text to draw
      * @param font Font to use
      * @param getTextArea A function for determining text draw bounds when text size is known
      */
    def drawSingleLineTextPositioned(text: String, font: Font)(getTextArea: Size => Bounds) =
    {
        // Sets up the graphics context
        prepareForTextDraw(font)
        val metrics = graphics.getFontMetrics
        
        val textSize = Size(metrics.stringWidth(text), metrics.getHeight)
        val textArea = getTextArea(textSize)
        
        // Checks whether the text needs to be scaled
        val topLeft = textArea.position
        val scaling = (textArea.size / textSize).toVector
        
        if (scaling ~== Vector3D.identity)
            graphics.drawString(text, topLeft.x.round.toInt, topLeft.y.round.toInt + metrics.getAscent)
        else
            transformed(AffineTransformation(topLeft.toVector, scaling = scaling))
                .graphics.drawString(text, 0, metrics.getAscent)
    }
    
    /**
      * Draws a piece of text. Specified bounds affect the positioning and possibly scaling.
      * @param text Text to draw
      * @param font Font to use
      * @param betweenLinesMargin Margin placed between lines of text if multiple lines are used (default = 0.0)
      * @param getTextArea A function for determining the bounds of the drawn text when text size is known
      * @param getLineX A function for calculating the left x-coordinate of a text line based on the line width and the
      *                 maximum line width.
      */
    def drawMultilineTextPositioned(text: String, font: Font, betweenLinesMargin: Double = 0.0)
                                   (getTextArea: Size => Bounds)(getLineX: (Double, Double) => Double) =
    {
        val lines = text.linesIterator.toVector
        if (lines.size > 1)
            drawTextLinesPositioned(lines, font, betweenLinesMargin)(getTextArea)(getLineX)
        else
            drawSingleLineTextPositioned(text, font)(getTextArea)
    }
    
    /**
      * Draws multiple lines of text. Specified bounds affect the positioning and possibly scaling.
      * @param lines Lines of text to draw
      * @param font Font to use
      * @param betweenLinesMargin Margin placed between lines of text if multiple lines are used (default = 0.0)
      * @param getTextArea A function for determining the bounds of the drawn text when text size is known
      * @param getLineX A function for calculating the left x-coordinate of a text line based on the line width and the
      *                 maximum line width.
      */
    def drawTextLinesPositioned(lines: Seq[String], font: Font, betweenLinesMargin: Double = 0.0)
                               (getTextArea: Size => Bounds)(getLineX: (Double, Double) => Double) =
    {
        // TODO: WET WET
        if (lines.nonEmpty)
        {
            // Sets up the graphics context
            prepareForTextDraw(font)
            val metrics = graphics.getFontMetrics
    
            val lineHeight = metrics.getAscent
            val lineWidths = lines.map(metrics.stringWidth)
            val textWidth = lineWidths.max
            val textHeight = lines.size * lineHeight + ((lines.size - 1) max 0) * betweenLinesMargin
            
            val textSize = Size(textWidth, textHeight)
            val textArea = getTextArea(textSize)
    
            // Checks whether the text needs to be scaled
            val topLeft = textArea.position
            val scaling = (textArea.size / textSize).toVector
            
            // Draws the lines
            // TODO: WET WET
            if (scaling ~== Vector3D.identity)
                lines.foreachWithIndex { (line, index) =>
                    graphics.drawString(line, (topLeft.x + getLineX(lineWidths(index), textWidth)).round.toInt,
                        (topLeft.y + index * (lineHeight + betweenLinesMargin)).round.toInt + lineHeight)
                }
            else
            {
                val drawer = transformed(AffineTransformation(topLeft.toVector, scaling = scaling)).graphics
                lines.foreachWithIndex { (line, index) =>
                    drawer.drawString(line, getLineX(lineWidths(index), textWidth).round.toInt,
                        (index * (lineHeight + betweenLinesMargin)).round.toInt + lineHeight)
                }
            }
        }
    }
    
    /**
      * Draws an image
      * @param image The image to be drawn
      * @param position The position where the image origin is drawn
      * @return Whether the image was fully loaded and drawn
      */
    def drawImage(image: utopia.genesis.image.Image, position: Point): Boolean = image.drawWith(this, position)
    
    /**
      * Draws an image
      * @param image The image to be drawn
      * @param position The position of the image's top left corner (default = (0, 0))
      * @return Whether the image was fully loaded and drawn
      */
    def drawImage(image: Image, position: Point = Point.origin): Boolean =
    {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        
        graphics.drawImage(image, position.x.toInt, position.y.toInt, null)
    }
    
    /**
      * Copies a region of the drawn area to another location
      * @param area Area that is copied
      * @param translation The amount of translation applied to the area
      */
    def copyArea(area: Bounds, translation: TwoDimensional[Double]) =
    {
        if (translation.dimensions2D.exists { _ != 0 })
            graphics.copyArea(
                area.x.round.toInt, area.y.round.toInt, area.width.round.toInt, area.height.round.toInt,
                translation.x.round.toInt, translation.y.round.toInt)
    }
    
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
      * Creates a transformed copy of this drawer, affecting all future paint operations
      * @param transform A transformation to apply
      * @return A transformed copy of this drawer
      */
    def transformed(transform: JavaAffineTransformConvertible): Drawer = transformed(transform.toJavaAffineTransform)
    
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
    
    /**
      * Prepares this drawer for drawing text. This is only necessary with the "raw" text draw option.
      * @param font Font used when drawing text
      * @param textColor Color to use when drawing text
      * @return A copy of this drawer, prepared to draw text
      */
    def forTextDrawing(font: Font, textColor: Color) =
    {
        val drawer = withEdgeColor(textColor)
        drawer.prepareForTextDraw(font)
        drawer
    }
    
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