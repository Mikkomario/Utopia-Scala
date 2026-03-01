package utopia.echo.model.vastai.instance

import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.{IntType, StringType}
import utopia.flow.generic.casting.ValueUnwraps._

object SshKey extends FromModelFactoryWithSchema[SshKey]
{
	// ATTRIBUTES   ---------------------
	
	override val schema: ModelDeclaration = ModelDeclaration("id" -> IntType, "public_key" -> StringType)
	
	
	// IMPLEMENTED  ---------------------
	
	override protected def fromValidatedModel(model: Model): SshKey =
		apply(model("id"), model("public_key"), model("name"))
}

/**
 * Represents an SSH key registered on a rented instance
 * @param id ID of this SSH key
 * @param publicKey Wrapped public key
 * @param name Name given to this key, if applicable
 * @author Mikko Hilpinen
 * @since 01.03.2026, v1.5
 */
case class SshKey(id: Int, publicKey: String, name: String = "")
{
	override def toString = publicKey
}
