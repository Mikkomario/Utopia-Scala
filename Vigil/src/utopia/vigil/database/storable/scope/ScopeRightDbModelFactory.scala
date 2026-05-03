package utopia.vigil.database.storable.scope

import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.{DbPropertyDeclaration, Table}
import utopia.vigil.database.props.scope.{ScopeRightDbProps, ScopeRightDbPropsWrapper}
import utopia.vigil.model.partial.scope.ScopeRightData
import utopia.vigil.model.stored.scope.ScopeRight

import java.time.Instant

object ScopeRightDbModelFactory
{
	// OTHER	--------------------
	
	/**
	  * @return A factory for constructing scope right database models
	  */
	def apply(table: Table, dbProps: ScopeRightDbProps) = ScopeRightDbModelFactoryImpl(table, dbProps)
	
	
	// NESTED	--------------------
	
	/**
	  * Used for constructing ScopeRightDbModel instances and for inserting scope rights to the 
	  * database
	  * @param table             Table targeted by these models
	  * @param scopeRightDbProps Properties which specify how the database interactions are performed
	  * @author Mikko Hilpinen
	  * @since 01.05.2026, v0.1
	  */
	case class ScopeRightDbModelFactoryImpl(table: Table, scopeRightDbProps: ScopeRightDbProps) 
		extends ScopeRightDbModelFactory with ScopeRightDbPropsWrapper
	{
		// ATTRIBUTES	--------------------
		
		override val id = DbPropertyDeclaration("id", index)
		
		
		// IMPLEMENTED	--------------------
		
		override def apply(data: ScopeRightData): ScopeRightDbModel = 
			apply(None, Some(data.scopeId), Some(data.created), Some(data.usable))
		
		override def withCreated(created: Instant) = apply(created = Some(created))
		
		override def withId(id: Int) = apply(id = Some(id))
		
		override def withScopeId(scopeId: Int) = apply(scopeId = Some(scopeId))
		
		override def withUsable(usable: Boolean) = apply(usable = Some(usable))
		
		override protected def complete(id: Value, data: ScopeRightData) = ScopeRight(id.getInt, data)
		
		
		// OTHER	--------------------
		
		/**
		  * @param id scope right database id
		  * @return Constructs a new scope right database model with the specified properties
		  */
		def apply(id: Option[Int] = None, scopeId: Option[Int] = None, created: Option[Instant] = None, 
			usable: Option[Boolean] = None): ScopeRightDbModel = 
			_ScopeRightDbModel(table, scopeRightDbProps, id, scopeId, created, usable)
	}
	
	/**
	  * Used for interacting with ScopeRights in the database
	  * @param table   Table interacted with when using this model
	  * @param dbProps Configurations of the interacted database properties
	  * @param id      scope right database id
	  * @author Mikko Hilpinen
	  * @since 01.05.2026, v0.1
	  */
	private case class _ScopeRightDbModel(table: Table, dbProps: ScopeRightDbProps, id: Option[Int] = None, 
		scopeId: Option[Int] = None, created: Option[Instant] = None, usable: Option[Boolean] = None) 
		extends ScopeRightDbModel
	{
		// IMPLEMENTED	--------------------
		
		/**
		  * @param id      Id to assign to the new model (default = currently assigned id)
		  * @param scopeId scope id to assign to the new model (default = currently assigned value)
		  * @param created created to assign to the new model (default = currently assigned value)
		  * @param usable  usable to assign to the new model (default = currently assigned value)
		  * @return Copy of this model with the specified scope right properties
		  */
		override protected def copyScopeRight(id: Option[Int] = id, scopeId: Option[Int] = scopeId, 
			created: Option[Instant] = created, usable: Option[Boolean] = usable) = 
			copy(id = id, scopeId = scopeId, created = created, usable = usable)
	}
}

/**
  * Common trait for factories yielding scope right database models
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightDbModelFactory 
	extends ScopeRightDbModelFactoryLike[ScopeRightDbModel, ScopeRight, ScopeRightData]

