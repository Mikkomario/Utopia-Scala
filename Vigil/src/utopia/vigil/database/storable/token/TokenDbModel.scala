package utopia.vigil.database.storable.token

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.Now
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.{Deprecates, HasIdProperty}
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.sql.Condition
import utopia.vault.store.{FromIdFactory, HasId}
import utopia.vigil.database.VigilTables
import utopia.vigil.model.factory.token.TokenFactory
import utopia.vigil.model.partial.token.TokenData
import utopia.vigil.model.stored.token.Token

import java.time.Instant

/**
  * Used for constructing TokenDbModel instances and for inserting tokens to the database
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object TokenDbModel 
	extends StorableFactory[TokenDbModel, Token, TokenData] with FromIdFactory[Int, TokenDbModel] 
		with HasIdProperty with TokenFactory[TokenDbModel] with Deprecates
{
	// ATTRIBUTES	--------------------
	
	override val id = DbPropertyDeclaration("id", index)
	/**
	  * Database property used for interacting with template ids
	  */
	lazy val templateId = property("templateId")
	/**
	  * Database property used for interacting with hashes
	  */
	lazy val hash = property("hash")
	/**
	  * Database property used for interacting with parent ids
	  */
	lazy val parentId = property("parentId")
	/**
	  * Database property used for interacting with names
	  */
	lazy val name = property("name")
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	/**
	  * Database property used for interacting with expiration times
	  */
	lazy val expires = property("expires")
	/**
	  * Database property used for interacting with revoke times
	  */
	val revoked = property("revoked")
	
	private val notRevokedCondition = revoked.isNull
	
	
	// IMPLEMENTED	--------------------
	
	override def table = VigilTables.token
	
	override def activeCondition: Condition = notRevokedCondition && !(expires <= Now)
	
	override def apply(data: TokenData): TokenDbModel =
		apply(None, Some(data.templateId), data.hash, data.parentId, data.name, Some(data.created), 
			data.expires, data.revoked)
	
	override def withCreated(created: Instant) = apply(created = Some(created))
	override def withExpires(expires: Instant) = apply(expires = Some(expires))
	override def withHash(hash: String) = apply(hash = hash)
	override def withId(id: Int) = apply(id = Some(id))
	override def withName(name: String) = apply(name = name)
	override def withParentId(parentId: Int) = apply(parentId = Some(parentId))
	override def withRevoked(revoked: Instant) = apply(revoked = Some(revoked))
	override def withTemplateId(templateId: Int) = apply(templateId = Some(templateId))
	
	override protected def complete(id: Value, data: TokenData) = Token(id.getInt, data)
}

/**
  * Used for interacting with Tokens in the database
  * @param id token database id
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenDbModel(id: Option[Int] = None, templateId: Option[Int] = None, hash: String = "",
                        parentId: Option[Int] = None, name: String = "", created: Option[Instant] = None,
                        expires: Option[Instant] = None, revoked: Option[Instant] = None)
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, TokenDbModel] 
		with TokenFactory[TokenDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(TokenDbModel.id.name -> id, TokenDbModel.templateId.name -> templateId, 
			TokenDbModel.hash.name -> hash, TokenDbModel.parentId.name -> parentId, 
			TokenDbModel.name.name -> name, TokenDbModel.created.name -> created, 
			TokenDbModel.expires.name -> expires, TokenDbModel.revoked.name -> revoked)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = TokenDbModel.table
	
	override def withCreated(created: Instant) = copy(created = Some(created))
	override def withExpires(expires: Instant) = copy(expires = Some(expires))
	override def withHash(hash: String) = copy(hash = hash)
	override def withId(id: Int) = copy(id = Some(id))
	override def withName(name: String) = copy(name = name)
	override def withParentId(parentId: Int) = copy(parentId = Some(parentId))
	override def withRevoked(revoked: Instant) = copy(revoked = Some(revoked))
	override def withTemplateId(templateId: Int) = copy(templateId = Some(templateId))
}

