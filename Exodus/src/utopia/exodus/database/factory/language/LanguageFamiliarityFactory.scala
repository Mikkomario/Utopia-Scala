package utopia.exodus.database.factory.language

import utopia.exodus.database.Tables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading language familiarity data from the DB
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
object LanguageFamiliarityFactory extends FromValidatedRowModelFactory[LanguageFamiliarity]
{
	override def table = Tables.languageFamiliarity
	
	override protected def fromValidatedModel(model: Model[Constant]) = LanguageFamiliarity(model("id"),
		model("orderIndex"))
}
