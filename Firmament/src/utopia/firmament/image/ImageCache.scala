package utopia.firmament.image

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.{Cache, ReleasingCache}
import utopia.flow.operator.Identity
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.vector.size.Size

import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object ImageCache
{
	/**
	  * Creates a cache that temporarily stores read images
	  * @param imageReadDirectory Directory from which images are read (implied root path for all cache keys)
	  * @param cacheDuration Duration how long read items should be strongly referenced
	  * @param exc Implicit execution context (used in scheduling cache-releases)
	  * @param log Implicit logging implementation
	  *            (used for logging image read-failures as well as possibly other more unexpected errors)
	  * @return A cache for read images
	  */
	def apply(imageReadDirectory: Path, cacheDuration: FiniteDuration = 3.minutes)
	         (implicit exc: ExecutionContext, log: Logger) =
		mapping(imageReadDirectory, cacheDuration)(Identity)
	/**
	  * Creates a cache for reading and storing single color icons
	  * @param imageReadDirectory Directory from which images are read (implied root path for all cache keys)
	  * @param standardIconSize Maximum size of all icons. None if no maximum should be specified (default).
	  * @param cacheDuration Duration how long read items should be strongly referenced
	  * @param exc Implicit execution context (used in scheduling cache-releases)
	  * @param log Implicit logging implementation
	  *            (used for logging image read-failures as well as possibly other more unexpected errors)
	  * @return A cache that yields icons
	  */
	def icons(imageReadDirectory: Path, standardIconSize: Option[Size] = None,
	          cacheDuration: FiniteDuration = 3.minutes)
	         (implicit exc: ExecutionContext, log: Logger) =
	{
		val f = standardIconSize match {
			case Some(size) => i: Image => SingleColorIcon(i.fittingWithin(size), size)
			case None => i: Image => SingleColorIcon(i)
		}
		mapping(imageReadDirectory, cacheDuration)(f)
	}
	
	/**
	  * Creates a cache that converts read images into other data types
	  * @param imageReadDirectory Directory from which images are read (implied root path for all cache keys)
	  * @param cacheDuration Duration how long read items should be strongly referenced
	  * @param f A mapping function applied to the read images
	  * @param exc Implicit execution context (used in scheduling cache-releases)
	  * @param log Implicit logging implementation
	  *            (used for logging image read-failures as well as possibly other more unexpected errors)
	  * @tparam A Type of cached items (mapping results)
	  * @return A cache for reading processed items
	  */
	def mapping[A <: AnyRef](imageReadDirectory: Path, cacheDuration: FiniteDuration = 3.minutes)
	                        (f: Image => A)
	                        (implicit exc: ExecutionContext, log: Logger) =
		new ImageCache[A](imageReadDirectory, cacheDuration)(f)
}

/**
  * Used for caching images and models derived from images
  * @author Mikko Hilpinen
  * @since 4.5.2020, Reflection v1.2
  * @param imageReadDirectory Path to the directory from which the images are read
  * @param cacheDuration How long icons are strongly referenced after first requested (default = 3 minutes)
  * @param f Mapping function applied to read images **before** they're cached
  * @param exc Implicit execution context used for scheduling cache releases
 * @param log Implicit logging implementation for possible icon read failures
  */
class ImageCache[+A <: AnyRef](val imageReadDirectory: Path,cacheDuration: FiniteDuration = 3.minutes)
                              (f: Image => A)
                              (implicit exc: ExecutionContext, log: Logger)
	extends Cache[String, A]
{
	// ATTRIBUTES	--------------------------
	
	private val cache: Cache[String, A] = ReleasingCache
		.after(cacheDuration) { imgName: String =>
			f(Image.readFrom(imageReadDirectory/imgName).getOrElseLog(Image.empty))
		}
		// Appends the .png portion to image names, if missing
		.mapKeys { imgName: String => if (imgName.contains('.')) imgName else s"$imgName.png" }
	
	
	// IMPLEMENTED	--------------------------
	
	override def cachedValues: Iterable[A] = cache.cachedValues
	
	override def cached(key: String): Option[A] = cache.cached(key)
	/**
	  * @param iconName Name of searched icon.
	  *                 If file type is not present (i.e. icon name doesn't contain a period (.)),
	  *                 ".png" suffix is added implicitly.
	  * @return An icon with specified name
	  */
	override def apply(iconName: String) = cache(iconName)
	
	
	// OTHER    ----------------------------
	
	/**
	  * Creates a new cache with a modified data-mapping function.
	  *
	  * When using this function, this cache should not be used anymore.
	  * If both this and the resulting cache will be used, consider creating another cache that utilizes this cache
	  * instead.
	  * This is because in this method, the resulting cache is a completely new instance and doesn't utilize this cache.
	  * I.e. unprocessed items won't be cached by the resulting cache.
	  *
	  * @param f A mapping function to apply (in addition to this cache's current mapping function)
	  * @tparam B Type of mapping results
	  * @return A cache that caches and yields processed items.
	  *         Doesn't cache unprocessed items.
	  */
	def mapValues[B <: AnyRef](f: A => B) = new ImageCache[B](imageReadDirectory, cacheDuration)(i => f(this.f(i)))
}
