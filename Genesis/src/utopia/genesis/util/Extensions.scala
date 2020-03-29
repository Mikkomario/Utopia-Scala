package utopia.genesis.util

import java.awt.Graphics2D
import java.awt.AlphaComposite
import scala.collection.mutable.ListBuffer

/**
 * This object contains some implicit extensions introduced in Genesis
 * @author Mikko Hilpinen
 * @since 24.12.2016
 */
object Extensions
{
    implicit class DoubleWithAlmostEquals[C](val d: C)(implicit f: C => Double) extends ApproximatelyEquatable[C]
    {
        /**
         * Checks if the two double numbers are approximately equal using 0.00001 precision
         */
        def ~==(d2: C) = (d -d2).abs < 0.00001
    }
    
    implicit class seqWithAlmostEquals[B, C](val s: Seq[C])(implicit f: C => ApproximatelyEquatable[B]) extends ApproximatelyEquatable[Seq[B]]
    {
        def ~==(s2: Seq[B]): Boolean =
        {
            if (s.size == s2.size)
                s.indices.forall { i => s(i) ~== s2(i) }
            else
                false
        }
    }
    
    implicit class SeqWithDistinctMap[T](val s: Seq[T]) extends AnyVal
    {
        def withDistinct(compare: (T, T) => Boolean) = 
        {
            val buffer = ListBuffer[T]()
            s.foreach { element => if (!buffer.exists { compare(_, element) }) buffer += element }
            buffer.toVector
        }
    }
    
    implicit class FailableIterable[T](val c: Iterable[T]) extends AnyVal
    {
        /**
         * This function maps values like a normal map function, but terminates immediately if 
         * None is returned by the transform function
         * @return The mapped collection or none if mapping failed for any element
         */
        def mapOrFail[B](f: T => Option[B]): Option[Vector[B]] = 
        {
            val iterator = c.iterator
            val buffer = Vector.newBuilder[B]
            
            while (iterator.hasNext)
            {
                val result = f(iterator.next())
                if (result.isDefined)
                {
                    buffer += result.get
                }
                else
                {
                    return None
                }
            }
            
            Some(buffer.result())
        }
    }
    
    @deprecated("Replaced with the new drawer class", "v0.3")
    implicit class ExtendedGraphics(val g: Graphics2D) extends AnyVal
    {
        /**
         * Changes the drawing opacity / alpha for the future drawing
         * @param alpha The alpha value [0, 1]
         */
        def setAlpha(alpha: Double) = g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, alpha.toFloat))
    }
}