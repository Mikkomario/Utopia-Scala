package utopia.vigil.model.partial.token

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.vigil.model.factory.token.TokenFactory

import java.time.Instant

object TokenData extends FromModelFactoryWithSchema[TokenData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("templateId", IntType, Single("template_id")), 
			PropertyDeclaration("hash", StringType), PropertyDeclaration("parentId", IntType, 
			Single("parent_id"), isOptional = true), PropertyDeclaration("name", StringType, 
			isOptional = true), PropertyDeclaration("created", InstantType, isOptional = true), 
			PropertyDeclaration("expires", InstantType, isOptional = true), PropertyDeclaration("revoked", 
			InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		TokenData(valid("templateId").getInt, valid("hash").getString, valid("parentId").int, 
			valid("name").getString, valid("created").getInstant, valid("expires").instant, 
			valid("revoked").instant)
}

/**
  * Represents a token that may be used for authorizing certain actions
  * @param templateId ID of the template used when creating this token
  * @param hash       Hashed version of this token
  * @param parentId   ID of the token that was used to generate this token
  * @param name       Name of this token. May be empty.
  * @param created    Time when this token was created
  * @param expires    Time when this token automatically expires. None if this token doesn't 
  *                   expire automatically.
  * @param revoked    Time when this token was revoked.
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenData(templateId: Int, hash: String, parentId: Option[Int] = None, name: String = "", 
	created: Instant = Now, expires: Option[Instant] = None, revoked: Option[Instant] = None) 
	extends TokenFactory[TokenData] with ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this token has already been deprecated
	  */
	def isDeprecated = revoked.isDefined
	
	/**
	  * Whether this token is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("templateId" -> templateId, "hash" -> hash, "parentId" -> parentId, "name" -> name, 
			"created" -> created, "expires" -> expires, "revoked" -> revoked))
	
	override def withCreated(created: Instant) = copy(created = created)
	
	override def withExpires(expires: Instant) = copy(expires = Some(expires))
	
	override def withHash(hash: String) = copy(hash = hash)
	
	override def withName(name: String) = copy(name = name)
	
	override def withParentId(parentId: Int) = copy(parentId = Some(parentId))
	
	override def withRevoked(revoked: Instant) = copy(revoked = Some(revoked))
	
	override def withTemplateId(templateId: Int) = copy(templateId = templateId)
}

