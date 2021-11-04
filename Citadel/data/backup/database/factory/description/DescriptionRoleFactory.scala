package utopia.citadel.database.factory.description

import utopia.citadel.database.Tables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.partial.description.DescriptionRoleData
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading description roles from the database
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1.0
  */
object DescriptionRoleFactory extends FromValidatedRowModelFactory[DescriptionRole]
{
	override def table = Tables.descriptionRole
	
	override protected def fromValidatedModel(model: Model) = DescriptionRole(model("id"),
		DescriptionRoleData(model("jsonKeySingular"), model("jsonKeyPlural")))
}
