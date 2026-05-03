package utopia.vigil.database.access.token.template

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.template.Filterable
import utopia.vigil.database.storable.token.TokenTemplateDbModel

/**
  * Common trait for access points which may be filtered based on token template properties
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait FilterTokenTemplates[+Repr] extends Filterable[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines token template database properties
	  */
	def model = TokenTemplateDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param name name to target
	  * @return Copy of this access point that only includes token templates with the specified name
	  */
	def withName(name: String) = filter(model.name.column <=> name)
	
	/**
	  * @param names Targeted names
	  * @return Copy of this access point that only includes token templates where name is within the 
	  * specified value set
	  */
	def withNames(names: Iterable[String]) = filter(model.name.column.in(names))
}

