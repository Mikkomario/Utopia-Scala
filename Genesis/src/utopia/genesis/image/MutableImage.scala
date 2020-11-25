package utopia.genesis.image

import java.awt.image.{BufferedImage, BufferedImageOp}

import utopia.flow.datastructure.mutable.{MutableLazy, ResettableLazy}
import utopia.genesis.color.Color
import utopia.genesis.image.transform.{ImageTransform, IncreaseContrast, Invert}
import utopia.genesis.shape.shape2D.{Bounds, Point, Size, Vector2D}
import utopia.genesis.shape.template.Dimensional

/**
  * A mutable image implementation that allows direct modifications
  * @author Mikko Hilpinen
  * @since 25.11.2020, v2.4
  */
class MutableImage(initialSource: Option[BufferedImage], initialScaling: Vector2D = Vector2D.identity,
				   initialAlpha: Double = 1.0, initialOrigin: Option[Point] = None) extends ImageLike
{
	// ATTRIBUTES	-------------------------------
	
	private var _source = initialSource
	
	private val pixelsCache = MutableLazy {
		source match
		{
			case Some(image) => PixelTable.fromBufferedImage(image)
			case None => PixelTable.empty
		}
	}
	
	private var _alpha = (initialAlpha max 0.0) min 1.0
	
	var scaling = initialScaling
	var specifiedOrigin = initialOrigin
	def specifiedOrigin_=(newOrigin: Point): Unit = specifiedOrigin = Some(newOrigin)
	
	
	// COMPUTED	-----------------------------------
	
	def immutableCopy = source match
	{
		case Some(img) =>
			val colorModel = img.getColorModel
			val isAlphaPremultiplied = colorModel.isAlphaPremultiplied
			val raster = img.copyData(null)
			Image(new BufferedImage(colorModel, raster, isAlphaPremultiplied, null)
				.getSubimage(0, 0, img.getWidth, img.getHeight), scaling, alpha, specifiedOrigin)
		case None => Image.empty
	}
	
	def sourceResolutionOrigin_=(newOrigin: Point) = specifiedOrigin = newOrigin
	
	def origin_=(newOrigin: Point) = specifiedOrigin = newOrigin / scaling
	
	
	// IMPLEMENTED	-------------------------------
	
	override def source = _source
	
	override def alpha = _alpha
	def alpha_=(newAlpha: Double) = _alpha = (newAlpha max 0.0) min 1.0
	
	override def sourceResolution = source match
	{
		case Some(s) => Size(s.getWidth, s.getHeight)
		case None => Size.zero
	}
	
	override def size = sourceResolution * scaling
	def size_=(newSize: Size) = resize(newSize, preserveShape = false)
	
	override def bounds = Bounds(-origin, size)
	
	override def isEmpty = source.isEmpty
	
	override def pixels = pixelsCache.value
	
	override def preCalculatedPixels = pixelsCache.current
	
	
	// OTHER	----------------------------------
	
	def *=(mod: Double) = scaling *= mod
	
	def *=(mod: Dimensional[Double]) = scaling *= mod
	
	def /=(div: Double) = scaling /= div
	
	// TODO: Add overlay with +
	
	def updateAlpha(f: Double => Double) = alpha = f(alpha)
	
	def updateSpecifiedOrigin(f: Option[Point] => Option[Point]) = specifiedOrigin = f(specifiedOrigin)
	
	def updateSourceResolutionOrigin(f: Point => Point) = sourceResolutionOrigin = f(sourceResolutionOrigin)
	
	def updateOrigin(f: Point => Point) = origin = f(origin)
	
	def centerOrigin() = specifiedOrigin = sourceResolution.toPoint / 2
	
	def downscale() = scaling = scaling.map { _ min 1 }
	
	def upscale() = scaling = scaling.map { _ max 1 }
	
	def clearScaling() = scaling = Vector2D.identity
	
	def updatePixelTable(f: PixelTable => PixelTable) =
	{
		if (source.isDefined)
		{
			val newPixels = f(pixels)
			_source = Some(newPixels.toBufferedImage)
			pixelsCache.value = newPixels
		}
	}
	
	def updatePixels(f: Color => Color) = updatePixelTable { _.map(f) }
	
	def resize(newSize: Size, preserveShape: Boolean = true) =
	{
		if (preserveShape)
			*=((newSize.width / width) min (newSize.height / height))
		else
			*=(newSize / size)
	}
	
	def resizeToFill(area: Size) = if (size.nonZero) *=((area / size).dimensions2D.max)
	
	def resizeToFit(area: Size) = if (size.nonZero) *=((area / size).dimensions2D.min)
	
	def flipHorizontally() =
	{
		updatePixelTable { _.flippedHorizontally }
		updateSpecifiedOrigin { _.map { o => Point(sourceResolution.width - o.x, o.y) } }
	}
	
	def flipVertically() =
	{
		updatePixelTable { _.flippedVertically }
		updateSpecifiedOrigin { _.map { o => Point(o.x, sourceResolution.height - o.y) } }
	}
	
	def transformWith(transformation: ImageTransform) = transformation(this)
	
	def increaseContrast() = transformWith(IncreaseContrast)
	
	def invert() = transformWith(Invert)
	
	def filterWith(op: BufferedImageOp) = source.foreach { source =>
		val destination = new BufferedImage(source.getWidth, source.getHeight, source.getType)
		op.filter(source, destination)
		this._source = Some(destination)
		pixelsCache.reset()
	}
}
