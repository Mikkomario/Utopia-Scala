package utopia.reflection.image

import utopia.flow.collection.immutable.caching.cache.ReleasingCache

import java.nio.file.Path
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Size

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Used for caching single color icons
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.2
  * @param imageReadDirectory Path to the directory from which the images are read
  * @param standardIconSize Maximum size of all icons. None if no maximum should be specified (default).
  * @param cacheDuration How long icons are strongly referenced after first requested (default = 3 minutes)
  * @param exc Implicit execution context used for scheduling cache releases
  */
class SingleColorIconCache(val imageReadDirectory: Path, standardIconSize: Option[Size] = None,
						   cacheDuration: FiniteDuration = 3.minutes)(implicit exc: ExecutionContext)
{
	// ATTRIBUTES	--------------------------
	
	private val cache = ReleasingCache.after[String, SingleColorIcon](cacheDuration) { imgName =>
		val image = Image.readOrEmpty(imageReadDirectory/imgName)
		standardIconSize match {
			case Some(size) => new SingleColorIcon(image.fittingWithin(size))
			case None => new SingleColorIcon(image)
		}
	}
	
	
	// OTHER	------------------------------
	
	/**
	  * @param iconName Name of searched icon (including file extension)
	  * @return An icon with specified name
	  */
	def apply(iconName: String) = cache(iconName)
}
