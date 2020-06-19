package utopia.exodus.database.factory.user

import utopia.exodus.database.factory.language.LanguageFactory
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.combined.user.FullUserLanguage
import utopia.metropolis.model.stored.language.Language
import utopia.vault.nosql.factory.LinkedFactory

/**
  * Used for reading user languages with language data included
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
object FullUserLanguageFactory extends LinkedFactory[FullUserLanguage, Language]
{
	// IMPLEMENTED	--------------------------
	
	override def childFactory = LanguageFactory
	
	override def apply(model: Model[Constant], child: Language) =
		UserLanguageFactory(model).map { FullUserLanguage(_, child) }
	
	override def table = UserLanguageFactory.table
}
