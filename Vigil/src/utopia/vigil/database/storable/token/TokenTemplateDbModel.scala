package utopia.vigil.database.storable.token

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.Duration
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}
import utopia.vigil.database.VigilTables
import utopia.vigil.model.enumeration.ScopeGrantType
import utopia.vigil.model.factory.token.TokenTemplateFactory
import utopia.vigil.model.partial.token.TokenTemplateData
import utopia.vigil.model.stored.token.TokenTemplate

import java.time.Instant

/**
  * Used for constructing TokenTemplateDbModel instances and for inserting token templates to the 
  * database
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object TokenTemplateDbModel 
	extends StorableFactory[TokenTemplateDbModel, TokenTemplate, TokenTemplateData] 
		with FromIdFactory[Int, TokenTemplateDbModel] with HasIdProperty 
		with TokenTemplateFactory[TokenTemplateDbModel]
{
	// ATTRIBUTES	--------------------
	
	override val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with names
	  */
	lazy val name = property("name")
	
	/**
	  * Database property used for interacting with scope grant types
	  */
	lazy val scopeGrantType = property("scopeGrantTypeId")
	
	/**
	  * Database property used for interacting with durations
	  */
	lazy val duration = property("durationMillis")
	
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = VigilTables.tokenTemplate
	
	override def apply(data: TokenTemplateData): TokenTemplateDbModel = 
		apply(None, data.name, Some(data.scopeGrantType), data.duration, Some(data.created))
	
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	override def withDuration(duration: Duration) = apply(duration = Some(duration))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	override def withName(name: String) = apply(name = name)
	
	override def withScopeGrantType(scopeGrantType: ScopeGrantType) = apply(scopeGrantType = 
		Some(scopeGrantType))
	
	override protected def complete(id: Value, data: TokenTemplateData) = TokenTemplate(id.getInt, data)
}

/**
  * Used for interacting with TokenTemplates in the database
  * @param id token template database id
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenTemplateDbModel(id: Option[Int] = None, name: String = "", 
	scopeGrantType: Option[ScopeGrantType] = None, duration: Option[Duration] = None, 
	created: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, TokenTemplateDbModel] 
		with TokenTemplateFactory[TokenTemplateDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(TokenTemplateDbModel.id.name -> id, TokenTemplateDbModel.name.name -> name, 
			TokenTemplateDbModel.scopeGrantType.name -> scopeGrantType.map[Value] { e => e.id }.getOrElse(Value.empty), 
			TokenTemplateDbModel.duration.name -> duration.map { _.toMillis }, 
			TokenTemplateDbModel.created.name -> created)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = TokenTemplateDbModel.table
	
	override def withCreated(created: Instant) = copy(created = Some(created))
	
	override def withDuration(duration: Duration) = copy(duration = Some(duration))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	override def withName(name: String) = copy(name = name)
	
	override def withScopeGrantType(scopeGrantType: ScopeGrantType) = copy(scopeGrantType = 
		Some(scopeGrantType))
}

