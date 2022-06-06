package utopia.vault.coder.model.data

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.enumeration.CombinationType

/**
  * Contains basic information for constructing a model combination
  * @author Mikko Hilpinen
  * @since 14.10.2021, v1.2
  * @param combinationType Type of this combination
  * @param name Name of the combined class
  * @param parentClass Parent part class
  * @param childClass Child part class
  * @param parentAlias Alias used for the parent class instances (optional)
  * @param childAlias Alias used for the child class instances (optional)
  * @param description Documentation for this combination
  * @param isAlwaysLinked Whether this combination is always linked (default = false)
  */
case class CombinationData(combinationType: CombinationType, name: Name, parentClass: Class, childClass: Class,
                           parentAlias: Option[Name] = None, childAlias: Option[Name] = None, description: String = "",
                           isAlwaysLinked: Boolean = false)
{
	/**
	  * @return Name used for the parent class instances
	  */
	def parentName = parentAlias.getOrElse(parentClass.name)
	/**
	  * @return Name used for the child class instances
	  */
	def childName = childAlias.getOrElse { childClass.name }
	
	/**
	  * @return Name of the sub-package for the combined model / factory
	  */
	def packageName = parentClass.packageName
	
	/**
	  * @return Author of this combined class
	  */
	def author = parentClass.author.notEmpty.getOrElse(childClass.author)
}
