package utopia.firmament.factory

import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.enumeration.SizeCategory._

/**
  * Common trait for factories that can produce different-sized items
  * @tparam A Type of constructed items
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.4.1
  */
trait FromSizeCategoryFactory[+A]
{
	// ABSTRACT -------------------------
	
	/**
	  * @param size Size of the resulting item
	  * @return A new item with that (general) size
	  */
	def apply(size: SizeCategory): A
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return A very small item
	  */
	def verySmall = apply(VerySmall)
	/**
	  * @return A small item
	  */
	def small = apply(Small)
	/**
	  * @return A medium-sized item
	  */
	def medium = apply(Medium)
	/**
	  * @return A large item
	  */
	def large = apply(Large)
	/**
	  * @return A very large item
	  */
	def veryLarge = apply(VeryLarge)
}
