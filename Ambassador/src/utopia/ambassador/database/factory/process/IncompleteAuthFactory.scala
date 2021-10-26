package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.database.model.process.IncompleteAuthModel
import utopia.ambassador.model.partial.process.IncompleteAuthData
import utopia.ambassador.model.stored.process.IncompleteAuth
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading IncompleteAuth data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object IncompleteAuthFactory 
	extends FromValidatedRowModelFactory[IncompleteAuth] with FromRowFactoryWithTimestamps[IncompleteAuth] 
		with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = IncompleteAuthModel.nonDeprecatedCondition
	
	override def table = AmbassadorTables.incompleteAuth
	
	override def fromValidatedModel(valid: Model[Constant]) = 
		IncompleteAuth(valid("id").getInt, IncompleteAuthData(valid("serviceId").getInt, 
			valid("code").getString, valid("token").getString, valid("expires").getInstant, 
			valid("created").getInstant))
}

