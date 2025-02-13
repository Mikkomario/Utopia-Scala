package utopia.paradigm.color

import utopia.paradigm.color.ColorRole._

/**
  * Common trait for factories that construct different-colored items based on a color role
  * @tparam A Type of constructed items
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.7.2
  */
trait FromColorRoleFactory[+A]
{
	// ABSTRACT ------------------------
	
	/**
	  * @param role A color role
	  * @return An item colored based on the specified role
	  */
	def apply(role: ColorRole): A
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return An item with the primary color-scheme color
	  */
	def primary = apply(Primary)
	/**
	  * @return An item with the secondary color-scheme color
	  */
	def secondary = apply(Secondary)
	/**
	  * @return An item with the tertiary color-scheme color
	  */
	def tertiary = apply(Tertiary)
	
	/**
	  * @return A grayscale item
	  */
	def gray = apply(Gray)
	
	/**
	  * @return An item with a warning-themed color
	  */
	def warning = apply(Warning)
	/**
	  * @return An item with a failure/error-themed color
	  */
	def failure = apply(Failure)
	/**
	  * @return An item with a success-themed color
	  */
	def success = apply(Success)
	/**
	  * @return An item with an info-related color
	  */
	def info = apply(Info)
}
