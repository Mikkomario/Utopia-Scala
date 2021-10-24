package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.database.model.auth.SessionTokenModel
import utopia.exodus.model.partial.auth.SessionTokenData
import utopia.exodus.model.stored.auth.SessionToken
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading SessionToken data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object SessionTokenFactory 
	extends FromRowModelFactory[SessionToken] with FromRowFactoryWithTimestamps[SessionToken] 
		with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = SessionTokenModel.nonDeprecatedCondition
	
	override def table = ExodusTables.sessionToken
	
	override def apply(model: template.Model[Property]) = 
	{
		table.validate(model).map{ valid => 
			val modelStylePreference = valid("modelStyleId").int.flatMap(ModelStyle.findForId)
			SessionToken(valid("id").getInt, SessionTokenData(valid("userId").getInt, 
				valid("token").getString, valid("expires").getInstant, valid("deviceId").int, 
				modelStylePreference, valid("created").getInstant, valid("loggedOut").instant))
		}
	}
}

