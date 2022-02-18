package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.database.model.auth.TokenModel
import utopia.exodus.model.partial.auth.TokenData
import utopia.exodus.model.stored.auth.Token
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading token data from the DB
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object TokenFactory 
	extends FromRowModelFactory[Token] with FromRowFactoryWithTimestamps[Token] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = TokenModel.nonDeprecatedCondition
	
	override def table = ExodusTables.token
	
	override def apply(model: template.Model[Property]) = {
		table.validate(model).map{ valid => 
			val modelStylePreference = valid("modelStyleId").int.flatMap(ModelStyle.findForId)
			Token(valid("id").getInt, TokenData(valid("typeId").getInt, valid("hash").getString, 
				valid("ownerId").int, valid("deviceId").int, modelStylePreference, valid("expires").instant, 
				valid("created").getInstant, valid("deprecatedAfter").instant))
		}
	}
}

