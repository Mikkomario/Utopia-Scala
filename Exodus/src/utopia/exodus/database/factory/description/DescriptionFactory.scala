package utopia.exodus.database.factory.description

import utopia.exodus.database.Tables
import utopia.exodus.database.model.description.DescriptionModel
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.Description
import utopia.vault.nosql.factory.FromValidatedRowModelFactory

/**
  * Used for reading description data from the DB
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
object DescriptionFactory extends FromValidatedRowModelFactory[Description]
{
	// IMPLEMENTED	--------------------------------
	
	override protected def fromValidatedModel(model: Model[Constant]) = Description(model("id"),
		DescriptionData(model(this.model.descriptionRoleIdAttName), model("languageId"), model("text"),
			model("authorId")))
	
	override def table = Tables.description
	
	
	// COMPUTED	-------------------------------------
	
	def model = DescriptionModel
}
