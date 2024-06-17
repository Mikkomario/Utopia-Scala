package utopia.vault.nosql.template

import utopia.vault.model.template.HasTable

/**
  * A common trait for access points that use indexed tables
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait Indexed extends HasTable
{
	// COMPUTED	-----------------------
	
	/**
	  * @return The index column in the primary table
	  */
	def index = table.primaryColumn.get
}
