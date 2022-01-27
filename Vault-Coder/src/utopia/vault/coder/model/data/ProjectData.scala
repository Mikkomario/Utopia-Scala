package utopia.vault.coder.model.data

import utopia.flow.datastructure.immutable.Pair
import utopia.flow.util.Version
import utopia.vault.coder.model.scala.Package

/**
  * Contains project classes, enumerations etc.
  * @author Mikko Hilpinen
  * @since 14.10.2021, v1.2
  * @param projectName Name of this project (db part if has two names)
  * @param modelPackage Package that contains the models and enumerations for this project
  * @param databasePackage Package that contains the database interactions for this project
  * @param enumerations Enumerations in this project
  * @param classes Classes in this project
  * @param combinations Combinations in this project
  * @param version Project version
  * @param modelCanReferToDB Whether model classes are allowed to refer to database classes
  */
case class ProjectData(projectName: String, modelPackage: Package, databasePackage: Package, enumerations: Vector[Enum],
                       classes: Vector[Class], combinations: Vector[CombinationData], version: Option[Version],
                       modelCanReferToDB: Boolean)
{
	// COMPUTED ------------------------------
	
	/**
	  * @return Whether this data set is completely empty
	  */
	def isEmpty = enumerations.isEmpty && classes.isEmpty && combinations.isEmpty
	
	/**
	  * @return A copy of this project data with only classes remaining
	  */
	def onlyClasses = copy(enumerations = Vector())
	/**
	  * @return A copy of this project data with only classes remaining (excluding combinations)
	  */
	def onlyBaseClasses = copy(enumerations = Vector(), combinations = Vector())
	/**
	  * @return A copy of this project data with only enumerations remaining
	  */
	def onlyEnumerations = copy(classes = Vector(), combinations = Vector())
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param filter A filter to apply
	  * @return Copy of this projects data that keeps items that are somehow matched with that filter
	  */
	def filter(filter: Filter) =
	{
		val baseFilteredClasses = classes.filter { c => filter(c.name) || filter(c.packageName) }
		val (filteredClasses, filteredCombos) = comboInclusiveClasses(baseFilteredClasses) { c => filter(c.name) }
		copy(classes = filteredClasses, enumerations = enumerations.filter { e => filter(e.name) },
			combinations = filteredCombos)
	}
	
	/**
	  * @param filter A filter to apply
	  * @return A copy of this data with only classes remaining which match the filter by their name
	  */
	def filterByClassName(filter: Filter) = filterByClass { c => filter(c.name) }
	/**
	  * @param filter A filter to apply
	  * @return A copy of this data with only classes remaining which match the filter by their package name
	  */
	def filterByPackage(filter: Filter) = filterByClass { c => filter(c.packageName) }
	/**
	  * @param f A filter to apply
	  * @return A copy of this data with only classes remaining which match the filter
	  */
	def filterByClass(f: Class => Boolean) = filterByClassOrCombo(f) { _ => false }
	/**
	  * @param includeClass A function that returns true for classes that should be included in the results
	  *                     (also includes their related combinations)
	  * @param includeCombo A function that returns true for combinations that should be included in the results
	  *                     (also includes their classes)
	  * @return A copy of this data with only classes remaining which match the filter
	  */
	def filterByClassOrCombo(includeClass: Class => Boolean)(includeCombo: CombinationData => Boolean) =
	{
		val remainingClasses = classes.filter(includeClass)
		val (filteredClasses, filteredCombos) = comboInclusiveClasses(remainingClasses)(includeCombo)
		copy(enumerations = Vector(), classes = filteredClasses, combinations = filteredCombos)
	}
	
	/**
	  * @param filter A filter to apply
	  * @return A copy of this data with only enumerations remaining which match the filter by their name
	  */
	def filterByEnumName(filter: Filter) =
		copy(enumerations = enumerations.filter { e => filter(e.name) }, classes = Vector(), combinations = Vector())
	
	private def comboInclusiveClasses(filteredClasses: Vector[Class])
	                                 (comboInclusionCondition: CombinationData => Boolean) =
	{
		val filteredCombos = combinations.filter { c => comboInclusionCondition(c) ||
			filteredClasses.contains(c.childClass) || filteredClasses.contains(c.parentClass) }
		val comboClasses = filteredCombos.flatMap { c => Pair(c.parentClass, c.childClass) }
		(filteredClasses ++ comboClasses).distinct -> filteredCombos
	}
}