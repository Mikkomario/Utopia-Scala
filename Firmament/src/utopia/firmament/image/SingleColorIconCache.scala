package utopia.firmament.image

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.ReleasingCache
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Size

import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Used for caching single color icons
  * @author Mikko Hilpinen
  * @since 4.5.2020, Reflection v1.2
  * @param imageReadDirectory Path to the directory from which the images are read
  * @param standardIconSize Maximum size of all icons. None if no maximum should be specified (default).
  * @param cacheDuration How long icons are strongly referenced after first requested (default = 3 minutes)
  * @param exc Implicit execution context used for scheduling cache releases
 * @param log Implicit logging implementation for possible icon read failures
  */
class SingleColorIconCache(val imageReadDirectory: Path, standardIconSize: Option[Size] = None,
						   cacheDuration: FiniteDuration = 3.minutes)
                          (implicit exc: ExecutionContext, log: Logger)
{
	// ATTRIBUTES	--------------------------
	
	private val cache = ReleasingCache
		.after[String, SingleColorIcon](cacheDuration) { imgName =>
			val image = Image.readFrom(imageReadDirectory/imgName).getOrElseLog(Image.empty)
			standardIconSize match {
				case Some(size) => SingleColorIcon(image.fittingWithin(size), size)
				case None => SingleColorIcon(image)
			}
		}
		// Appends the .png portion to image names, if missing
		.mapKeys { imgName: String => if (imgName.contains('.')) imgName else s"$imgName.png" }
	
	
	// OTHER	------------------------------
	
	/**
	  * @param iconName Name of searched icon (including file extension)
	  * @return An icon with specified name
	  */
	def apply(iconName: String) = cache(iconName)
}
