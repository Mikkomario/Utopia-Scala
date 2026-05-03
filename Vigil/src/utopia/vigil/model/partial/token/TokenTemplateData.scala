package utopia.vigil.model.partial.token

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.DurationType
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.{Duration, Now}
import utopia.vigil.model.enumeration.ScopeGrantType
import utopia.vigil.model.factory.token.TokenTemplateFactory

import java.time.Instant

object TokenTemplateData extends FromModelFactory[TokenTemplateData]
{
	// ATTRIBUTES	--------------------
	
	lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("name", StringType, isOptional = true), 
			PropertyDeclaration("scopeGrantType", IntType, Single("scope_grant_type")), 
			PropertyDeclaration("duration", DurationType, isOptional = true), PropertyDeclaration("created", 
			InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override def apply(model: HasProperties) = {
		schema.validate(model).flatMap { valid => 
			ScopeGrantType.fromValue(valid("scopeGrantType")).map { scopeGrantType => 
				TokenTemplateData(valid("name").getString, scopeGrantType, valid("duration").duration, 
					valid("created").getInstant)
			}
		}
	}
}

/**
  * A template or a mold for creating new tokens
  * @param name           Name of this template. May be empty.
  * @param scopeGrantType Way the scope-granting functions in this template
  * @param duration       Duration of the created tokens. None if infinite.
  * @param created        Time when this token template was added to the database
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenTemplateData(name: String, scopeGrantType: ScopeGrantType, duration: Option[Duration] = None, 
	created: Instant = Now) 
	extends TokenTemplateFactory[TokenTemplateData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("name" -> name, "scopeGrantType" -> scopeGrantType.id, "duration" -> duration, 
			"created" -> created))
	
	override def withCreated(created: Instant) = copy(created = created)
	
	override def withDuration(duration: Duration) = copy(duration = Some(duration))
	
	override def withName(name: String) = copy(name = name)
	
	override def withScopeGrantType(scopeGrantType: ScopeGrantType) = copy(scopeGrantType = scopeGrantType)
}

