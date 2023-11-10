package utopia.paradigm.measurement

/**
 * Extensions related to distances
 * @author Mikko Hilpinen
 * @since Genesis 24.6.2020, v2.3
 */
object DistanceExtensions
{
	/**
	 * Used for easily converting numbers to distances by adding a unit
	 * @param i Original number
	 * @tparam A Number type
	 */
	implicit class DistanceNumber[A](val i: A) extends AnyVal
	{
		private def double(implicit n: Numeric[A]) = n.toDouble(i)
		
		/**
		 * @return this amount of millimeters
		 */
		def mm(implicit n: Numeric[A]) = Distance.ofMillis(double)
		/**
		 * @return this amount of centimeters
		 */
		def cm(implicit n: Numeric[A]) = Distance.ofCm(double)
		/**
		 * @return This amount of meters
		 */
		def m(implicit n: Numeric[A]) = Distance.ofMeters(double)
		/**
		  * @return This amount of kilometers
		  */
		def km(implicit n: Numeric[A]) = Distance.ofKilometers(double)
		
		/**
		 * @return This amount of inches
		 */
		def inches(implicit n: Numeric[A]) = Distance.ofInches(double)
		
		/**
		 * @return This amount of feet
		 */
		def feet(implicit n: Numeric[A]) = Distance.ofFeet(double)
		
		/**
		  * @param n Implicit numeric interface
		  * @param ppi Pixels per inch in this context
		  * @return This length in pixels (converted to a distance)
		  */
		def pixels(implicit n: Numeric[A], ppi: Ppi) = ppi.pixelLength * double
	}
}
