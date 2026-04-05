package utopia.genesis.util

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.util.NumberExtensions._
import utopia.genesis.graphics.TextDrawHeight.Standard
import utopia.genesis.graphics.{DrawSettings, StrokeSettings, TextDrawHeight}
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment.{BottomRight, Top, TopLeft}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.Triangle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
 * Used for generating visual charts
 * @author Mikko Hilpinen
 * @since 05.04.2026, v4.2.4
 */
object Chart
{
	/**
	 * Draws a point chart
	 * @param data Data to visualize
	 * @param font Font to use when drawing text
	 * @param targetImageLength Targeted length of the longer image side, in pixels
	 * @param linePixelInterval Interval between grid lines, in pixels
	 * @param xLabel Label describing the X axis (default = empty)
	 * @param yLabel Label describing the Y axis (default = empty)
	 * @return An image visualizing this chart
	 */
	def points(data: Iterable[(Point, Color)], font: Font, targetImageLength: Double,
	           linePixelInterval: Double = 16, margin: Int = 8, xLabel: String = "", yLabel: String = "") =
	{
		// Case: Nothing to visualize => Yields an empty image
		if (data.isEmpty)
			Image.empty
		else {
			// Determines the scale & image size
			val minPoint = data.iterator.map { _._1 }.reduce { _ topLeft _ }
			val maxPoint = data.iterator.map { _._1 }.reduce { _ bottomRight _ }
			
			val area = (maxPoint - minPoint.topLeft(Point.zero)).toSize
			val pixelUnit = area.maxDimension / targetImageLength
			val valueMargin = margin * pixelUnit
			
			val zero = Point(area.width - maxPoint.x + valueMargin, maxPoint.y + valueMargin)
			val zeroPixels = zero / pixelUnit
			
			// Determines how numbers are textualized
			val valueToString = {
				if (area.minDimension <= 5)
					{ d: Double => d.roundDecimals(1).toString }
				else
					{ d: Double => d.round.toInt.toString }
			}
			
			val imageSize = (area / pixelUnit).ceil + Size.square(margin * 2)
			val drawPixelArea = Bounds(Point.origin, imageSize).shrunk(margin * 2)
			Image.paint(imageSize) { drawer =>
				// Draws the background
				DrawSettings.onlyFill(Color.white).use { implicit ds =>
					drawer.draw(Bounds(Point.origin, imageSize))
				}
				
				drawer.antialiasing.use { drawer =>
					val textDrawer = drawer.forTextDrawing(font.toAwt)
					implicit val textDrawHeight: TextDrawHeight = Standard
					
					// Draws the grid
					StrokeSettings(Color.textBlackDisabled).toDrawSettings.use { implicit ds =>
						Pair(X -> Positive, Y -> Negative).foreach { case (axis, positiveDir) =>
							val baseLinePixels = zeroPixels(axis.perpendicular)
							Sign.values
								// Determines the grid line locations
								.mapAndMerge { sign =>
									val valuesIter = Iterator
										.iterate(zeroPixels(axis)) { _ + linePixelInterval * sign.modifier }
									sign match {
										case Positive =>
											val max = drawPixelArea.maxAlong(axis)
											valuesIter.takeWhile { _ <= max }
										case Negative =>
											val min = drawPixelArea.minAlong(axis)
											valuesIter.takeWhile { _ <= min }
									}
								} { _ ++ _ }
								.foreach { pixelLen =>
									// Above / right of the baseline
									drawer.draw(Line(
										Point(pixelLen, baseLinePixels, axis),
										Point(pixelLen, drawPixelArea.extremeAlong(positiveDir.extreme, axis))
									))
									// Below / left of the baseline (includes margin for text)
									drawer.draw(Line(
										Point(pixelLen, baseLinePixels + valueMargin * positiveDir.opposite.modifier, axis),
										Point(pixelLen, drawPixelArea.extremeAlong(positiveDir.opposite.extreme, axis))
									))
									
									// Also draws the threshold values
									textDrawer.drawAround(valueToString(pixelLen * pixelUnit),
										Point(pixelLen, baseLinePixels, axis), Top)
								}
						}
					}
					
					// Draws the axes
					StrokeSettings(Color.textBlack).toDrawSettings.use { implicit ds =>
						drawer.draw(Line(Point(margin, zeroPixels.y), Point(imageSize.width - margin, zeroPixels.y)))
						drawer.draw(Line(Point(zeroPixels.x, margin), Point(zeroPixels.x, imageSize.height - margin)))
					}
					
					// Draws the labels & arrows
					DrawSettings.onlyFill(Color.textBlack).use { implicit ds =>
						val xTrianglePos = Point(drawPixelArea.rightX, zeroPixels.y)
						val yTrianglePos = Point(zeroPixels.x, drawPixelArea.topY)
						
						drawer.draw(Triangle(xTrianglePos, Pair(-2, 2).map { len => Vector2D(-4, len) }))
						drawer.draw(Triangle(yTrianglePos, Pair(-2, 2).map { len => Vector2D(len, 4) }))
						
						if (xLabel.nonEmpty)
							textDrawer.drawAround(xLabel, xTrianglePos - Y(margin / 2), BottomRight)
						if (yLabel.nonEmpty)
							textDrawer.drawAround(yLabel, yTrianglePos + X(margin / 2), TopLeft)
					}
					
					// Draws the values
					data.foreach { case (point, color) =>
						DrawSettings.onlyFill(color).use { implicit ds =>
							// Reverses the Y coordinates since charts have +Y on top
							drawer.draw(Circle(zeroPixels + (point - zero).mapY { -_ } / pixelUnit, 2))
						}
					}
				}
			}
		}
	}
}
