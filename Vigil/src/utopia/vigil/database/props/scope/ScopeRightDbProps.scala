package utopia.vigil.database.props.scope

import utopia.vault.model.immutable.{DbPropertyDeclaration, Table}
import utopia.vault.model.template.HasIdProperty

object ScopeRightDbProps
{
	// OTHER	--------------------
	
	/**
	  * @param table           Table operated using this configuration
	  * @param scopeIdPropName Name of the database property matching scope id (default = "scopeId")
	  * @param createdPropName Name of the database property matching created (default = "created")
	  * @param usablePropName  Name of the database property matching usable (default = "usable")
	  * @return A model which defines all scope right database properties
	  */
	def apply(table: Table, scopeIdPropName: String = "scopeId", createdPropName: String = "created", 
		usablePropName: String = "usable"): ScopeRightDbProps = 
		_ScopeRightDbProps(table, scopeIdPropName, createdPropName, usablePropName)
	
	
	// NESTED	--------------------
	
	/**
	  * @param table           Table operated using this configuration
	  * @param scopeIdPropName Name of the database property matching scope id (default = "scopeId")
	  * @param createdPropName Name of the database property matching created (default = "created")
	  * @param usablePropName  Name of the database property matching usable (default = "usable")
	  */
	private case class _ScopeRightDbProps(table: Table, scopeIdPropName: String = "scopeId", 
		createdPropName: String = "created", usablePropName: String = "usable") 
		extends ScopeRightDbProps
	{
		// ATTRIBUTES	--------------------
		
		override val id = DbPropertyDeclaration("id", index)
		
		override lazy val scopeId = DbPropertyDeclaration.from(table, scopeIdPropName)
		
		override lazy val created = DbPropertyDeclaration.from(table, createdPropName)
		
		override lazy val usable = DbPropertyDeclaration.from(table, usablePropName)
	}
}

/**
  * Common trait for classes which provide access to scope right database properties
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightDbProps extends HasIdProperty
{
	// ABSTRACT	--------------------
	
	/**
	  * Declaration which defines how scope id shall be interacted with in the database
	  */
	def scopeId: DbPropertyDeclaration
	
	/**
	  * Declaration which defines how created shall be interacted with in the database
	  */
	def created: DbPropertyDeclaration
	
	/**
	  * Declaration which defines how usable shall be interacted with in the database
	  */
	def usable: DbPropertyDeclaration
}

