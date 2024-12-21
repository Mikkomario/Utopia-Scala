package utopia.genesis.image

import utopia.flow.util.EitherExtensions._
import utopia.genesis.graphics.Drawer
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape3d.Matrix3D

import java.awt.image.BufferedImage

/**
  * Common trait for read only views into concrete images.
  * Concrete images are images that wrap a single buffered AWT image, which also support scaling transformations.
  * @author Mikko Hilpinen
  * @since 20.12.2024, v4.2
  */
trait ConcreteImageView extends ImageView
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Measurements of the original image data
	  */
	def sourceResolution: Size
	/**
	  * @return Scaling applied to this image
	  */
	def scaling: Vector2D
	
	/**
	  * @return A specifically set origin for this image. In the source image space (i.e. no scaling applied).
	  *         None if origin is unspecified.
	  */
	def specifiedOrigin: Option[Point]
	
	/**
	  * @return Wrapped mutable awt image
	  */
	protected def source: Option[BufferedImage]
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return The origin of this image that is relative to this image's source resolution and not necessarily
	  *         the current size.
	  */
	def sourceResolutionOrigin = specifiedOrigin.getOrElse(Point.origin)
	
	
	// IMPLEMENTED  -------------------
	
	override def origin: Point = specifiedOrigin match {
		case Some(o) => o * scaling
		case None => Point.origin
	}
	
	override def specifiesOrigin: Boolean = specifiedOrigin.isDefined
	
	override def maxScaling: Double = (Vector2D.identity / scaling).minDimension max 1.0
	
	override def toImage: ConcreteImage = toConcreteImage
	
	override def toString = {
		val alphaPortion = if (alpha == 1) "" else s" ${(alpha * 100).toInt}% Alpha"
		val originPortion = specifiedOrigin match {
			case Some(origin) => s" Origin at ${origin * scaling}"
			case None => ""
		}
		s"Image ($size$alphaPortion$originPortion)"
	}
	
	override def pixelAt(point: Point, relativeToOrigin: Boolean) = {
		val relativeToTopLeft = if (relativeToOrigin) point + origin else point
		pixels.lookup(relativeToTopLeft/scaling).getOrElse(Color.transparentBlack)
	}
	override def pixelsAt(area: Bounds, relativeToOrigin: Boolean) = {
		val relativeToTopLeft = if (relativeToOrigin) area - origin else area
		pixels.view(relativeToTopLeft / scaling)
	}
	
	override def drawWith(drawer: Drawer, position: Point = Point.origin, transformation: Option[Matrix2D] = None) = {
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
	override def drawSubImageWith(drawer: Drawer, subRegion: Bounds, position: Point = Point.origin) = {
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
