package utopia.genesis.shape.path

import utopia.genesis.animation.AnimationLike
import utopia.genesis.shape.path.Path.{CurvedPath, RepeatingPath, ReversePath}
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
	
	
	// NESTED   -------------------
	
	private class ReversePath[+P](original: Path[P]) extends Path[P]
	{
		override def start = original.end
		
		override def end = original.start
		
		override def apply(progress: Double) = original(1 - progress)
		
		override def reversed = original
	}
	
	private class CurvedPath[+P](original: Path[P], curve: AnimationLike[Double, Any]) extends Path[P]
	{
		override def start = original.start
		
		override def end = original.end
		
		override def apply(progress: Double) = original(curve(progress))
	}
	
	private class RepeatingPath[+P](original: Path[P], times: Int) extends Path[P]
	{
		override def start = original.start
		
		override def end = original.end
		
		override def apply(progress: Double) = original(progress * times)
	}
}

/**
  * Paths form a sequence of points. They have a specified start and end point
  * @author Mikko Hilpinen
  * @since 19.6.2019, v2.1+
  */
trait Path[+P] extends AnimationLike[P, Path]
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
	
	
	// IMPLEMENTED	-----------------
	
	override def reversed: Path[P] = new ReversePath[P](this)
	
	override def curved(curvature: AnimationLike[Double, Any]): Path[P] = new CurvedPath[P](this, curvature)
	
	override def map[B](f: P => B): Path[B] = MappedPath[P, B](this)(f)
	
	override def repeated(times: Int): Path[P] = new RepeatingPath[P](this, times)
}
