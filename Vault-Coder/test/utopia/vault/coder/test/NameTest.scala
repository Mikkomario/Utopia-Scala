package utopia.vault.coder.test

import utopia.vault.coder.model.data.{Name, NamingRules}
import utopia.vault.coder.model.enumeration.NamingConvention.CamelCase

/**
  * Tests some name-related functions
  * @author Mikko Hilpinen
  * @since 21.10.2022, v1.7.1
  */
object NameTest extends App
{
	implicit val naming: NamingRules = NamingRules.default
	
	private val manyPrefix = Name("Many", "Many", CamelCase.capitalized)
	private val accessTraitSuffix = Name("Access", "Access", CamelCase.capitalized)
	
	private def manyAccessTraitNameFrom(className: Name) = {
		// The "Many" -prefix is ignored if the class name already starts with "Many"
		if (className.pluralClassName.toLowerCase.startsWith("many"))
			className + accessTraitSuffix
		else
			(manyPrefix +: className) + accessTraitSuffix
	}
	
	val name = Name("RestrictedCustomer")
	
	// assert(manyAccessTraitNameFrom(name).plural == )
}
