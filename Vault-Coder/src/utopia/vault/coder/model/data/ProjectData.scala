package utopia.vault.coder.model.data

import utopia.vault.coder.model.scala.Package

/**
  * Contains project classes, enumerations etc.
  * @author Mikko Hilpinen
  * @since 14.10.2021, v1.2
  * @param basePackage Package that holds all project data
  * @param enumerations Enumerations in this project
  * @param classes Classes in this project
  * @param combinations Combinations in this project
  */
case class ProjectData(basePackage: Package, enumerations: Vector[Enum], classes: Vector[Class],
                       combinations: Vector[CombinationData])
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
		val filteredClasses = classes.filter { c => filter(c.name) || filter(c.packageName) }
		copy(classes = filteredClasses, enumerations = enumerations.filter { e => filter(e.name) },
			combinations = combinations.filter { c =>
				filteredClasses.contains(c.parentClass) || filteredClasses.contains(c.childClass) || filter(c.name) })
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
	def filterByClass(f: Class => Boolean) =
	{
		val remainingClasses = classes.filter(f)
		copy(enumerations =  Vector(), classes = remainingClasses,
			combinations = combinations.filter { c => remainingClasses.contains(c.parentClass) ||
				remainingClasses.contains(c.childClass) })
	}
	
	/**
	  * @param filter A filter to apply
	  * @return A copy of this data with only enumerations remaining which match the filter by their name
	  */
	def filterByEnumName(filter: Filter) =
		copy(enumerations = enumerations.filter { e => filter(e.name) }, classes = Vector(), combinations = Vector())
}