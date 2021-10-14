package utopia.vault.coder.model.data

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.enumeration.CombinationType
import utopia.vault.coder.model.enumeration.CombinationType.MultiCombined

/**
  * Contains basic information for constructing a model combination
  * @author Mikko Hilpinen
  * @since 14.10.2021, v1.2
  * @param combinationType Type of this combination
  * @param name Name of the combined class
  * @param parentClass Parent part class
  * @param childClass Child part class
  * @param parentAlias Alias used for the parent class instances (singular)
  * @param childAlias Alias used for the child class instances (singular or plural, depending on combination type)
  * @param isAlwaysLinked Whether this combination is always linked (default = false)
  */
case class CombinationData(combinationType: CombinationType, name: Name, parentClass: Class, childClass: Class,
                           parentAlias: String = "", childAlias: String = "", isAlwaysLinked: Boolean = false)
{
	/**
	  * @return Name used for the parent class instances
	  */
	def parentName = parentAlias.notEmpty.getOrElse(parentClass.name.singular)
	/**
	  * @return Name used for the child class instances
	  */
	def childName = childAlias.notEmpty.getOrElse {
		if (combinationType == MultiCombined)
			childClass.name.plural
		else
			childClass.name.singular
	}
	
	/**
	  * @return Name of the sub-package for the combined model / factory
	  */
	def packageName = parentClass.packageName
}
