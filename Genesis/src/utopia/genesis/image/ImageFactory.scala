package utopia.genesis.image

import utopia.flow.parse.AutoClose._
import utopia.genesis.graphics.Drawer
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, FileNotFoundException, InputStream}
import java.nio.file.{Files, Path}
import java.util.Base64
import javax.imageio.ImageIO
import scala.util.{Failure, Success, Try}

/**
  * Common trait for factories that construct images
  * @tparam I Type of constructed images
  * @author Mikko Hilpinen
  * @since 20.12.2024, v4.2
  */
trait ImageFactory[+I]
{
	// ABSTRACT --------------------------
	
	/**
	  * A zero sized image with no pixel data
	  */
	def empty: I
	
	/**
	  * Creates a new image
	  * @param image The original buffered image source
	  * @param scaling The scaling applied to the image
	  * @param alpha The maximum alpha value used when drawing this image [0, 1] (default = 1 = fully visible)
	  * @param origin The relative coordinate inside this image which is considered the drawing origin
	  *               (the (0,0) coordinate of this image). None if the origin should be left unspecified (default).
	  *               When unspecified, the (0,0) coordinate is placed in the top left corner.
	  *               Please notice that this origin is applied before scaling is applied, meaning that the specified
	  *               origin should always be in relation to the source resolution and not necessarily image size.
	  * @return A new image
	  */
	def apply(image: BufferedImage, scaling: Vector2D = Vector2D.identity, alpha: Double = 1.0,
	          origin: Option[Point] = None): I
	
	/**
	  * @param pixels A set of pixels
	  * @return An image based on those pixels
	  */
	def fromPixels(pixels: Pixels): I
	
	
	// OTHER    ---------------------------
	
	/**
	  * Converts an awt image to Genesis image class
	  * @param awtImage An awt image (buffered images are preferred because they can be simply wrapped)
	  * @return A genesis image
	  */
	def from(awtImage: java.awt.Image) = {
		awtImage match {
			case bufferedImage: BufferedImage => apply(bufferedImage) // Uses buffered image as is
			case otherType: java.awt.Image =>
				// Creates a new buffered image and draws original image on the new image
				val buffer = new BufferedImage(otherType.getWidth(null),
					otherType.getHeight(null), BufferedImage.TYPE_INT_ARGB)
				
				val g = buffer.createGraphics()
				g.drawImage(otherType, 0, 0, null)
				g.dispose()
				
				apply(buffer)
		}
	}
	
	/**
	  * Reads an image from a file
	  * @param path The path the image is read from
	  * @param readClass Class through which the resource is read from.
	  *                  Leave to None when reading files outside program resources. (Default = None)
	  * @return The read image wrapped in Try
	  */
	def readFrom(path: Path, readClass: Option[Class[_]] = None) = {
		// Checks that file exists (not available with class read method)
		if (readClass.isDefined || Files.exists(path)) {
			// ImageIO and class may return null. Image is read through class, if one is provided
			val readResult = Try { readClass.map { c => Option(c.getResourceAsStream(s"/${ path.toString }"))
					.flatMap { _.consume { stream => Option(ImageIO.read(stream)) } } }
				.getOrElse { Option(ImageIO.read(path.toFile)) } }
			
			readResult.flatMap {
				case Some(result) => Success(apply(result))
				case None =>
					Failure(new NoImageReaderAvailableException(s"Cannot read image from file: ${ path.toString }"))
			}
		}
		else
			Failure(new FileNotFoundException(s"No (image) file at: ${Try{ path.toAbsolutePath }.getOrElse(path) }"))
	}
	/**
	  * Reads an image from a file. If image is not available, returns an empty image.
	  * @param path       The path this image is read from
	  * @param readClass Class through which the resource is read from.
	  *                  Leave to None when reading files outside program resources. (Default = None)
	  * @return Read image, which may be empty
	  */
	def readOrEmpty(path: Path, readClass: Option[Class[_]] = None) = readFrom(path, readClass) match {
		case Success(img) => img
		case Failure(_) => empty
	}
	
	/**
	  * Creates a new image by drawing
	  * @param size Size of the image
	  * @param draw A function that will draw the image contents. The drawer is clipped to image bounds and
	  *             (0,0) is at the image top left corner.
	  * @tparam U Arbitrary result type
	  * @return Drawn image
	  */
	def paint[U](size: Size)(draw: Drawer => U): I = {
		// If some of the dimensions were 0, simply creates an empty image
		if (size.sign.isPositive) {
			// Creates the new buffer image
			val buffer = new BufferedImage(size.width.round.toInt, size.height.round.toInt, BufferedImage.TYPE_INT_ARGB)
			// Draws on the image
			Drawer(buffer.createGraphics()).consume(draw)
			// Wraps the buffer image
			apply(buffer)
		}
		else
			empty
	}
	
	/**
	  * Parses an image from its Base 64 encoded format.
	  * @param imageString A Base 64 encoded string which represents an image
	  * @return Parsed image. Failure if parsing failed.
	  */
	def fromBase64EncodedString(imageString: String) = Try {
		val imageBytes = Base64.getDecoder.decode(imageString)
		new ByteArrayInputStream(imageBytes).consume { stream => apply(ImageIO.read(stream)) }
	}
	
	/**
	  * Reads an image from an input stream.
	  * NB: Doesn't close the specified stream.
	  * @param stream A stream to read into an image
	  * @return An image read from the specified stream. Failure if image-reading failed.
	  */
	def fromStream(stream: InputStream) = Try { apply(ImageIO.read(stream)) }
	
	
	// NESTED	------------------------
	
	private class NoImageReaderAvailableException(message: String) extends RuntimeException(message)
}

