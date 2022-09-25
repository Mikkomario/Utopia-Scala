package utopia.citadel.database.factory.description

import utopia.citadel.database.CitadelTables
import utopia.citadel.database.model.description.DescriptionModel
import utopia.flow.generic.model.immutable.Model
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.Description
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading Description data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DescriptionFactory 
	extends FromValidatedRowModelFactory[Description] with FromRowFactoryWithTimestamps[Description] 
		with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = DescriptionModel.nonDeprecatedCondition
	
	override def table = CitadelTables.description
	
	override def fromValidatedModel(valid: Model) =
		Description(valid("id").getInt, DescriptionData(valid("roleId").getInt, valid("languageId").getInt, 
			valid("text").getString, valid("authorId").int, valid("created").getInstant, 
			valid("deprecatedAfter").instant))
}

