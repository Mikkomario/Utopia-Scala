package utopia.ambassador.database.model.process

import utopia.ambassador.database.factory.process.AuthPreparationScopeLinkFactory
import utopia.ambassador.model.partial.process.AuthPreparationScopeLinkData
import utopia.ambassador.model.stored.process.AuthPreparationScopeLink
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing AuthPreparationScopeLinkModel instances and for inserting AuthPreparationScopeLinks to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthPreparationScopeLinkModel 
	extends DataInserter[AuthPreparationScopeLinkModel, AuthPreparationScopeLink, 
		AuthPreparationScopeLinkData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains AuthPreparationScopeLink preparationId
	  */
	val preparationIdAttName = "preparationId"
	
	/**
	  * Name of the property that contains AuthPreparationScopeLink scopeId
	  */
	val scopeIdAttName = "scopeId"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains AuthPreparationScopeLink preparationId
	  */
	def preparationIdColumn = table(preparationIdAttName)
	
	/**
	  * Column that contains AuthPreparationScopeLink scopeId
	  */
	def scopeIdColumn = table(scopeIdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = AuthPreparationScopeLinkFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: AuthPreparationScopeLinkData) = 
		apply(None, Some(data.preparationId), Some(data.scopeId))
	
	override def complete(id: Value, data: AuthPreparationScopeLinkData) = 
		AuthPreparationScopeLink(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param id A AuthPreparationScopeLink id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param preparationId Id of the described OAuth preparation
	  * @return A model containing only the specified preparationId
	  */
	def withPreparationId(preparationId: Int) = apply(preparationId = Some(preparationId))
	
	/**
	  * @param scopeId Id of the requested scope
	  * @return A model containing only the specified scopeId
	  */
	def withScopeId(scopeId: Int) = apply(scopeId = Some(scopeId))
}

/**
  * Used for interacting with AuthPreparationScopeLinks in the database
  * @param id AuthPreparationScopeLink database id
  * @param preparationId Id of the described OAuth preparation
  * @param scopeId Id of the requested scope
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthPreparationScopeLinkModel(id: Option[Int] = None, preparationId: Option[Int] = None, 
	scopeId: Option[Int] = None) 
	extends StorableWithFactory[AuthPreparationScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def factory = AuthPreparationScopeLinkModel.factory
	
	override def valueProperties = 
	{
		import AuthPreparationScopeLinkModel._
		Vector("id" -> id, preparationIdAttName -> preparationId, scopeIdAttName -> scopeId)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param preparationId A new preparationId
	  * @return A new copy of this model with the specified preparationId
	  */
	def withPreparationId(preparationId: Int) = copy(preparationId = Some(preparationId))
	
	/**
	  * @param scopeId A new scopeId
	  * @return A new copy of this model with the specified scopeId
	  */
	def withScopeId(scopeId: Int) = copy(scopeId = Some(scopeId))
}

