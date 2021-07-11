package utopia.citadel.database.factory.language

import utopia.citadel.database.Tables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.stored.language.Language
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * A factory used for reading language data from the database
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1.0
  */
object LanguageFactory extends FromValidatedRowModelFactory[Language]
{
	// IMPLEMENTED	-------------------------------
	
	override def table = Tables.language
	
	override protected def fromValidatedModel(model: Model[Constant]) = Language(model("id").getInt,
		model("isoCode").getString)
}