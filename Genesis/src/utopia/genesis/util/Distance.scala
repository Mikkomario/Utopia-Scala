package utopia.genesis.util

/**
  * Common trait for items that have a measurable length
  * @author Mikko Hilpinen
  * @since 13.9.2019, v2.1+
  */
trait Distance
{
	/**
	  * @return the measured length of this item
	  */
	def length: Double
}
