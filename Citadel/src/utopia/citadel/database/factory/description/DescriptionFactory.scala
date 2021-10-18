package utopia.citadel.database.factory.description

import utopia.citadel.database.Tables
import utopia.citadel.database.model.description.DescriptionModel
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.Description
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading description data from the DB
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1.0
  */
object DescriptionFactory extends FromValidatedRowModelFactory[Description]
{
	// COMPUTED	-------------------------------------
	
	/**
	  * @return The model class linked with this factory
	  */
	def model = DescriptionModel
	
	
	// IMPLEMENTED	--------------------------------
	
	override protected def fromValidatedModel(model: Model[Constant]) = Description(model("id"),
		DescriptionData(model(this.model.roleIdAttName), model("languageId"), model(this.model.textAttName),
			model("authorId")))
	
	override def table = Tables.description
}
