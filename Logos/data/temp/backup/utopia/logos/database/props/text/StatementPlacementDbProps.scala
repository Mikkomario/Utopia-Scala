package utopia.logos.database.props.text

import utopia.vault.model.immutable.{DbPropertyDeclaration, Table}
import utopia.vault.model.template.HasIdProperty

object StatementPlacementDbProps
{
	// OTHER	--------------------
	
	/**
	  * @param table               Table operated using this configuration
	  * @param parentIdPropName    Name of the database property matching parent id (default = 
	  *                            "parentId")
	  * @param statementIdPropName Name of the database property matching statement id (default = 
	  *                            "statementId")
	  * @param orderIndexPropName  Name of the database property matching order index (default = 
	  *                            "orderIndex")
	  * @return A model which defines all statement placement database properties
	  */
	def apply(table: Table, parentIdPropName: String = "parentId", 
		statementIdPropName: String = "statementId", 
		orderIndexPropName: String = "orderIndex"): StatementPlacementDbProps = 
		_StatementPlacementDbProps(table, parentIdPropName, statementIdPropName, orderIndexPropName)
	
	
	// NESTED	--------------------
	
	/**
	  * @param table               Table operated using this configuration
	  * @param parentIdPropName    Name of the database property matching parent id (default = 
	  *                            "parentId")
	  * @param statementIdPropName Name of the database property matching statement id (default = 
	  *                            "statementId")
	  * @param orderIndexPropName  Name of the database property matching order index (default = 
	  *                            "orderIndex")
	  */
	private case class _StatementPlacementDbProps(table: Table, parentIdPropName: String = "parentId", 
		statementIdPropName: String = "statementId", orderIndexPropName: String = "orderIndex") 
		extends StatementPlacementDbProps
	{
		// ATTRIBUTES	--------------------
		
		override lazy val id = DbPropertyDeclaration("id", index)
		
		override lazy val parentId = DbPropertyDeclaration.from(table, parentIdPropName)
		
		override lazy val statementId = DbPropertyDeclaration.from(table, statementIdPropName)
		
		override lazy val orderIndex = DbPropertyDeclaration.from(table, orderIndexPropName)
	}
}

/**
  * Common trait for classes which provide access to statement placement database properties
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.4
  */
trait StatementPlacementDbProps extends TextPlacementDbProps with HasIdProperty
{
	// ABSTRACT	--------------------
	
	/**
	  * Declaration which defines how statement id shall be interacted with in the database
	  */
	def statementId: DbPropertyDeclaration
	
	
	// IMPLEMENTED	--------------------
	
	override def placedId = statementId
}

