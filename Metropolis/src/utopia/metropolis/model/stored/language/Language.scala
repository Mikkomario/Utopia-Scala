package utopia.metropolis.model.stored.language

import utopia.metropolis.model.partial.language.LanguageData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible, StyledStoredModelConvertible}

object Language extends StoredFromModelFactory[Language, LanguageData]
{
	override def dataFactory = LanguageData
}

/**
  * Represents a Language that has already been stored in the database
  * @param id id of this Language in the database
  * @param data Wrapped Language data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class Language(id: Int, data: LanguageData) extends StyledStoredModelConvertible[LanguageData]
{
	override protected def includeIdInSimpleModel = true
}

