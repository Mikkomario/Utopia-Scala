package utopia.genesis.image

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.genesis.graphics.Drawer
import utopia.paradigm.color.{Color, ColorShade}
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.{HasSize, Size}
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape3d.Matrix3D

import java.awt.image.BufferedImage

/**
  * A common trait for image implementations
  * @author Mikko Hilpinen
  * @since 25.11.2020, v2.4
  */
trait ImageLike extends HasSize
{
	// ABSTRACT	--------------------
	
	/**
	  * @return Wrapped mutable awt image
	  */
	protected def source: Option[BufferedImage]
	
	/**
	  * @return Scaling applied to this image
	  */
	def scaling: Vector2D
	/**
	  * @return Alpha used when drawing this image
	  */
	def alpha: Double
	
	/**
	  * @return Measurements of the original image data
	  */
	def sourceResolution: Size
	/**
	  * The bounds of this image when origin and size are both counted. The (0,0) coordinate is at the origin
	  * of this image.
	  */
	def bounds: Bounds
	/**
	  * @return A specifically set origin for this image. None if origin is unspecified.
	  */
	def specifiedOrigin: Option[Point]
	
	/**
	  * @return Whether this image is empty (0x0)
	  */
	def isEmpty: Boolean
	
	/**
	  * @return Pixel data for this image.
	  */
	def pixels: Pixels
	
	/**
	  * @return The average shade of this image
	  */
	def shade: ColorShade
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return Whether this image contains some data
	  */
	def nonEmpty = !isEmpty
	
	/**
	  * @return Whether this image has a specified origin
	  */
	def specifiesOrigin = specifiedOrigin.isDefined
	
	/**
	  * @return The origin of this image that is relative to this image's source resolution and not necessarily
	  *         the current size.
	  */
	def sourceResolutionOrigin = specifiedOrigin.getOrElse(Point.origin)
	
	/**
	  * @return The origin of this image that is relative to the image size and not source resolution
	  */
	def origin = specifiedOrigin.map { _ * scaling }.getOrElse(Point.origin)
	
	/**
	  * Calculates the length of this image from the origin to the specified direction (Eg. if origin is at
	  * the center of this image, returns width or height halved)
	  * @param direction Direction towards which the distance is counted
	  * @return Starting from this image's origin, the length of this image towards that direction
	  */
	def lengthTowards(direction: Direction2D) = direction.sign match {
		case Positive => size(direction.axis) - origin(direction.axis)
		case Negative => origin(direction.axis)
	}
	
	
	// IMPLEMENTED	----------------
	
	override def toString = {
		val alphaPortion = if (alpha == 1) "" else s" ${(alpha * 100).toInt}% Alpha"
		val originPortion = specifiedOrigin match {
			case Some(origin) => s" Origin at ${origin * scaling}"
			case None => ""
		}
		s"Image ($size$alphaPortion$originPortion)"
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param point Targeted point in this image <b>relative to this image's origin</b>
	  * @return A color of this image at the specified location
	  */
	def pixelAt(point: Point) = pixels.lookup(point).getOrElse(Color.transparentBlack)
	/**
	  * @param area Targeted area within this image. The (0,0) location is relative to the top left corner of this image
	  * @return An iterator that traverses through the pixels in that area
	  */
	def pixelsAt(area: Bounds) = pixels.view(area / scaling)
	
	/**
	  * @param area Targeted area within this image. The (0,0) is at the top left corner of this image
	  * @return The average luminosity of the pixels in the targeted area
	  */
	def averageLuminosityOf(area: Bounds) = pixelsAt(area).averageLuminosity
	/**
	  * @param area Targeted area within this image. The (0,0) is at the top left corner of this image
	  * @return The average relative luminance (perceived lightness) of the pixels in the targeted area
	  */
	def averageRelativeLuminanceOf(area: Bounds) = pixelsAt(area).averageRelativeLuminance
	
	/**
	  * Draws this image using a specific drawer
	  * @param drawer A drawer
	  * @param position The position where this image's origin is drawn (default = (0, 0))
	  * @param transformation An additional linear transformation to apply (optional)
	  * @return Whether this image was fully drawn
	  */
	def drawWith(drawer: Drawer, position: Point = Point.origin, transformation: Option[Matrix2D] = None) = {
		source.forall { s =>
			// Uses transformations in following order:
			// 1) Translates so that image origin is at (0,0)
			// 2) Performs scaling
			// 3) Performs additional transformation
			// 4) Positions correctly
			val baseTransform = specifiedOrigin match {
				case Some(origin) =>
					val originTransform = Matrix3D.translation(-origin)
					Right(if (scaling.isIdentity) originTransform else originTransform.scaled(scaling))
				case None => Left(if (scaling.isIdentity) None else Some(Matrix2D.scaling(scaling)))
			}
			val transformed = transformation match {
				case Some(t) =>
					baseTransform.mapBoth {
						case Some(b) => Some(b * t)
						case None => Some(t)
					} { _ * t }
				case None => baseTransform
			}
			val finalTransform = {
				if (position.isZero)
					transformed.rightOrMap {
						case Some(m) => m.to3D
						case None => Matrix3D.identity
					}
				else
					transformed.mapToSingle {
						case Some(t) => t.translated(position)
						case None => Matrix3D.translation(position)
					} { _.translated(position) }
			}
			
			// Performs the actual drawing
			val transformedDrawer = {
				if (alpha == 1)
					drawer * finalTransform
				else
					drawer.withAlpha(alpha) * finalTransform
			}
			transformedDrawer.drawAwtImage(s)
		}
	}
	/**
	  * Draws a region of this image using the specified drawer.
	  * @param drawer A drawer
	  * @param subRegion Region of this image that will be drawn, relative to the top left corner of this image.
	  * @param position The position where **this image's origin** is drawn (default = (0,0)).
	  * @return Whether the region was fully drawn already
	  */
	def drawSubImageWith(drawer: Drawer, subRegion: Bounds, position: Point = Point.origin) = {
		source.forall { img =>
			// Determines the targeted area within the source resolution
			val drawnRegion = subRegion / scaling
			// Places the drawn area so that it matches this image's scaling
			// and so that this image's origin will be placed at the specified point
			val targetArea = subRegion + (position - origin)
			drawer.drawAwtSubImage(img, drawnRegion, targetArea)
		}
	}
}
