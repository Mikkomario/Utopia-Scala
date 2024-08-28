package utopia.logos.database.props.text

import utopia.vault.model.immutable.{DbPropertyDeclaration, Table}
import utopia.vault.model.template.HasIdProperty

object TextPlacementDbProps
{
	// OTHER	--------------------
	
	/**
	  * @param table Table operated using this configuration
	  * @param parentIdPropName Name of the database property matching parent id (default = "parentId")
	  * @param placedIdPropName Name of the database property matching placed id (default = "placedId")
	  * @param orderIndexPropName Name of the database property matching order index (default = "orderIndex")
	  * @return A model which defines all text placement database properties
	  */
	def apply(table: Table, parentIdPropName: String = "parentId", placedIdPropName: String = "placedId", 
		orderIndexPropName: String = "orderIndex"): TextPlacementDbProps = 
		_TextPlacementDbProps(table, parentIdPropName, placedIdPropName, orderIndexPropName)
	
	
	// NESTED	--------------------
	
	/**
	  * @param table Table operated using this configuration
	  * @param parentIdPropName Name of the database property matching parent id (default = "parentId")
	  * @param placedIdPropName Name of the database property matching placed id (default = "placedId")
	  * @param orderIndexPropName Name of the database property matching order index (default = "orderIndex")
	  */
	private case class _TextPlacementDbProps(table: Table, parentIdPropName: String = "parentId", 
		placedIdPropName: String = "placedId", orderIndexPropName: String = "orderIndex") 
		extends TextPlacementDbProps
	{
		// ATTRIBUTES	--------------------
		
		override lazy val id = DbPropertyDeclaration("id", index)
		override lazy val parentId = DbPropertyDeclaration.from(table, parentIdPropName)
		override lazy val placedId = DbPropertyDeclaration.from(table, placedIdPropName)
		override lazy val orderIndex = DbPropertyDeclaration.from(table, orderIndexPropName)
	}
}

/**
  * Common trait for classes which provide access to text placement database properties
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacementDbProps extends HasIdProperty
{
	// ABSTRACT	--------------------
	
	/**
	  * Declaration which defines how parent id shall be interacted with in the database
	  */
	def parentId: DbPropertyDeclaration
	/**
	  * Declaration which defines how placed id shall be interacted with in the database
	  */
	def placedId: DbPropertyDeclaration
	/**
	  * Declaration which defines how order index shall be interacted with in the database
	  */
	def orderIndex: DbPropertyDeclaration
}

