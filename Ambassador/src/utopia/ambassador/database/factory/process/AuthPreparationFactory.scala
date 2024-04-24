package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.database.model.process.AuthPreparationModel
import utopia.ambassador.model.partial.process.AuthPreparationData
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.flow.generic.model.immutable.Model
import utopia.vault.model.immutable.DbPropertyDeclaration
import utopia.vault.nosql.factory.row.FromTimelineRowFactory
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading AuthPreparation data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthPreparationFactory 
	extends FromValidatedRowModelFactory[AuthPreparation] with FromTimelineRowFactory[AuthPreparation]
		with Deprecatable
{
	// COMPUTED ------------------------
	
	private def model = AuthPreparationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = AmbassadorTables.authPreparation
	
	override def timestamp: DbPropertyDeclaration = model.created
	override def nonDeprecatedCondition = model.nonDeprecatedCondition
	
	override def fromValidatedModel(valid: Model) =
		AuthPreparation(valid("id").getInt, AuthPreparationData(valid("userId").getInt, 
			valid("token").getString, valid("expires").getInstant, valid("clientState").string,
			valid("created").getInstant))
}

