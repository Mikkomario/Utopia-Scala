package utopia.reflection.shape

/**
  * These extensions allow easier creation of stack lengths & stack sizes
  * @author Mikko Hilpinen
  * @since 26.4.2019, v1+
  */
object LengthExtensions
{
	/*
	implicit class LengthInt(val i: Int) extends AnyVal
	{
		/**
		 * @return A stacklength that has no maximum or minimum, preferring this length
		 */
		def any = StackLength.any(i)
		
		/**
		 * @return A stacklength fixed to this length
		 */
		def fixed = StackLength.fixed(i)
		
		/**
		 * @return A stacklength maximized on this length with no minimum
		 */
		def downscaling = StackLength.downscaling(i)
		
		/**
		 * @return A stacklength minimized on this length with no maximum
		 */
		def upscaling = StackLength.upscaling(i)
		
		/**
		 * @param max Maximum length
		 * @return A stack length between this and maximum, preferring this
		 */
		def upTo(max: Int) = StackLength(i, i, max)
		
		/**
		 * @param min Minimum length
		 * @return A stack length between minimum and this, preferring this
		 */
		def downTo(min: Int) = StackLength(min, i, i)
	}*/
	
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
}
