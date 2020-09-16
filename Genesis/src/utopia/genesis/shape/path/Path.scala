package utopia.genesis.shape.path

import utopia.genesis.animation.Animation
import utopia.genesis.util.DistanceLike

object Path
{
	// TYPES    -----------------
	
	/**
	  * A common type of path that knows its length
	  */
	type PathWithDistance[+X] = Path[X] with DistanceLike
	
	
	// EXTENSIONS   -------------
	
	implicit class CombinablePath[A](val p: PathWithDistance[A]) extends AnyVal
	{
		/**
		  * Continues this path with another
		  * @param another Another path
		  * @tparam B The type of resulting path
		  * @return A path that starts with this path and continues with the another
		  */
		def +[B >: A](another: PathWithDistance[B]) = CompoundPath(Vector(p, another))
	}
}

/**
  * Paths form a sequence of points. They have a specified start and end point
  * @author Mikko Hilpinen
  * @since 19.6.2019, v2.1+
  */
trait Path[+P] extends Animation[P]
{
	// ABSTRACT	----------------
	
	/**
	  * @return The starting point of this path
	  */
	def start: P
	/**
	  * @return The end point of this path
	  */
	def end: P
	
	
	// OTHER	-----------------
	
	/**
	  * Maps the values of this path
	  * @param f A mapping function
	  * @tparam A Type of map function input
	  * @tparam B Type of map function output
	  * @return A mapped version of this path
	  */
	def map[A >: P, B](f: A => B) = MappedPath[A, B](this, f)
}
