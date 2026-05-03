package utopia.vigil.database.storable.token

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.DbPropertyDeclaration
import utopia.vigil.database.VigilTables
import utopia.vigil.database.props.scope.ScopeRightDbProps
import utopia.vigil.database.storable.scope.{ScopeRightDbModel, ScopeRightDbModelFactoryLike, ScopeRightDbModelLike}
import utopia.vigil.model.factory.token.TokenTemplateScopeFactory
import utopia.vigil.model.partial.token.TokenTemplateScopeData
import utopia.vigil.model.stored.token.TokenTemplateScope

import java.time.Instant

/**
  * Used for constructing TokenTemplateScopeDbModel instances and for inserting token template 
  * scopes to the database
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object TokenTemplateScopeDbModel 
	extends ScopeRightDbModelFactoryLike[TokenTemplateScopeDbModel, TokenTemplateScope, TokenTemplateScopeData] 
		with TokenTemplateScopeFactory[TokenTemplateScopeDbModel] with ScopeRightDbProps
{
	// ATTRIBUTES	--------------------
	
	override val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with scope ids
	  */
	override lazy val scopeId = property("scopeId")
	
	/**
	  * Database property used for interacting with template ids
	  */
	lazy val templateId = property("templateId")
	
	/**
	  * Database property used for interacting with creation times
	  */
	override lazy val created = property("created")
	
	/**
	  * Database property used for interacting with usable
	  */
	override lazy val usable = property("usable")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = VigilTables.tokenTemplateScope
	
	override def apply(data: TokenTemplateScopeData): TokenTemplateScopeDbModel = 
		apply(None, Some(data.scopeId), Some(data.templateId), Some(data.created), Some(data.usable))
	
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	override def withScopeId(scopeId: Int) = apply(scopeId = Some(scopeId))
	
	override def withTemplateId(templateId: Int) = apply(templateId = Some(templateId))
	
	override def withUsable(usable: Boolean) = apply(usable = Some(usable))
	
	override protected def complete(id: Value, data: TokenTemplateScopeData) = TokenTemplateScope(id.getInt, 
		data)
}

/**
  * Used for interacting with TokenTemplateScopes in the database
  * @param id token template scope database id
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenTemplateScopeDbModel(id: Option[Int] = None, scopeId: Option[Int] = None, 
	templateId: Option[Int] = None, created: Option[Instant] = None, usable: Option[Boolean] = None) 
	extends ScopeRightDbModel with ScopeRightDbModelLike[TokenTemplateScopeDbModel] 
		with TokenTemplateScopeFactory[TokenTemplateScopeDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		super[ScopeRightDbModelLike].valueProperties ++ 
			Single(TokenTemplateScopeDbModel.templateId.name -> templateId)
	
	
	// IMPLEMENTED	--------------------
	
	override def dbProps = TokenTemplateScopeDbModel
	
	override def table = TokenTemplateScopeDbModel.table
	
	/**
	  * @param id      Id to assign to the new model (default = currently assigned id)
	  * @param scopeId scope id to assign to the new model (default = currently assigned value)
	  * @param created created to assign to the new model (default = currently assigned value)
	  * @param usable  usable to assign to the new model (default = currently assigned value)
	  */
	override def copyScopeRight(id: Option[Int] = id, scopeId: Option[Int] = scopeId, 
		created: Option[Instant] = created, usable: Option[Boolean] = usable) = 
		copy(id = id, scopeId = scopeId, created = created, usable = usable)
	
	override def withTemplateId(templateId: Int) = copy(templateId = Some(templateId))
}

