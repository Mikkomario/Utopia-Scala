package utopia.reflection.shape

import utopia.genesis.shape.shape2D.Insets
import utopia.genesis.util.{Distance, Ppi}
import utopia.reflection.shape.stack.{StackInsets, StackLength}

/**
  * These extensions allow easier creation of stack lengths & stack sizes
  * @author Mikko Hilpinen
  * @since 26.4.2019, v1+
  */
object LengthExtensions
{
	implicit class LengthNumber[A](val i: A) extends AnyVal
	{
		private def double(implicit n: Numeric[A]) = n.toDouble(i)
		
		/**
		 * @return A stacklength that has no maximum or minimum, preferring this length
		 */
		def any(implicit n: Numeric[A]) = StackLength.any(double)
		
		/**
		 * @return A stacklength fixed to this length
		 */
		def fixed(implicit n: Numeric[A]) = StackLength.fixed(double)
		
		/**
		 * @return A stacklength maximized on this length with no minimum
		 */
		def downscaling(implicit n: Numeric[A]) = StackLength.downscaling(double)
		
		/**
		 * @return A stacklength minimized on this length with no maximum
		 */
		def upscaling(implicit n: Numeric[A]) = StackLength.upscaling(double)
		
		/**
		 * @param max Maximum length
		 * @return A stack length between this and maximum, preferring this
		 */
		def upTo(max: Double)(implicit n: Numeric[A]) = StackLength(double, double, max)
		
		/**
		 * @param min Minimum length
		 * @return A stack length between minimum and this, preferring this
		 */
		def downTo(min: Double)(implicit n: Numeric[A]) = StackLength(min, double, double)
	}
	
	implicit class StackableDistance(val d: Distance) extends AnyVal
	{
		/**
		 * @return A stack length that has no maximum or minimum, preferring this length
		 */
		def any(implicit ppi: Ppi) = StackLength.any(d.toPixels)
		
		/**
		 * @return A stack length fixed to this length
		 */
		def fixed(implicit ppi: Ppi) = StackLength.fixed(d.toPixels)
		
		/**
		 * @return A stack length maximized on this length with no minimum
		 */
		def downscaling(implicit ppi: Ppi) = StackLength.downscaling(d.toPixels)
		
		/**
		 * @return A stack length minimized on this length with no maximum
		 */
		def upscaling(implicit ppi: Ppi) = StackLength.upscaling(d.toPixels)
		
		/**
		 * @param max Maximum length
		 * @return A stack length between this and maximum, preferring this
		 */
		def upTo(max: Distance)(implicit ppi: Ppi) =
		{
			val p = d.toPixels
			StackLength(p, p, max.toPixels)
		}
		
		/**
		 * @param min Minimum length
		 * @return A stack length between minimum and this, preferring this
		 */
		def downTo(min: Distance)(implicit ppi: Ppi) =
		{
			val p = d.toPixels
			StackLength(min.toPixels, p, p)
		}
	}
	
	implicit class StackConvertibleInsets(val i: Insets) extends AnyVal
	{
		/**
		 * @return A stack-compatible copy of these insets that supports any other value but prefers these
		 */
		def any = toStackInsets { _.any }
		
		/**
		 * @return A stack-compatible copy of these insets that only allows these exact insets
		 */
		def fixed = toStackInsets { _.fixed }
		
		/**
		 * @return A stack-compatible copy of these insets that allows these or smaller insets
		 */
		def downscaling = toStackInsets { _.downscaling }
		
		/**
		 * @return A stack-compatible copy of these insets that allows these or larger insets
		 */
		def upscaling = toStackInsets { _.upscaling }
		
		/**
		 * @param min Minimum set of insets
		 * @return A set of stack insets that allows values below these insets, down to specified insets
		 */
		def downTo(min: Insets) = toStackInsetsWith(min) { (opt, min) => opt.downTo(min) }
		
		/**
		 * @param max Maximum set of insets
		 * @return A set of stack insets that allows values above these insets, up to specified insets
		 */
		def upTo(max: Insets) = toStackInsetsWith(max) { (opt, max) => opt.upTo(max) }
		
		
		// OTHER    ---------------------
		
		/**
		 * Converts these insets to stack insets by using the specified function
		 * @param f A function for converting a static length to a stack length
		 * @return Stack insets based on these insets
		 */
		def toStackInsets(f: Double => StackLength) = StackInsets(i.amounts.map { case (d, l) => d -> f(l) })
		
		/**
		 * Creates a set of stack insets by combining these insets with another set of insets and using the specified merge function
		 * @param other Another set of insets
		 * @param f Function for producing stack lengths
		 * @return A new set of stack insets
		 */
		def toStackInsetsWith(other: Insets)(f: (Double, Double) => StackLength) = stack.StackInsets(
			(i.amounts.keySet ++ other.amounts.keySet).map { d => d -> f(i(d), other(d)) }.toMap)
	}
}
