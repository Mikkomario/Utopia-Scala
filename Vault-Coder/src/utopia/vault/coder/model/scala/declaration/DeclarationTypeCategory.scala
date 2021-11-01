package utopia.vault.coder.model.scala.declaration

/**
  * An enumeration for general declaration types (instances vs. functions)
  * @author Mikko Hilpinen
  * @since 1.11.2021, v1.3
  */
sealed trait DeclarationTypeCategory

object DeclarationTypeCategory
{
	/**
	  * Category that contains objects, classes and traits
	  */
	case object Instance extends DeclarationTypeCategory
	/**
	  * Category that contains functions, values and variables
	  */
	case object Function extends DeclarationTypeCategory
}
