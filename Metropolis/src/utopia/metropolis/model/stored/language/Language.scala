package utopia.metropolis.model.stored.language

import utopia.metropolis.model.combined.description.LinkedDescription
import utopia.metropolis.model.combined.language.DescribedLanguage
import utopia.metropolis.model.partial.language.LanguageData
import utopia.metropolis.model.stored.description.DescriptionLink
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
	// IMPLEMENTED  -------------------------
	
	override protected def includeIdInSimpleModel = true
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param descriptions Descriptions concerning this language
	  * @return A described copy of this language
	  */
	def withDescriptions(descriptions: Set[LinkedDescription]) = DescribedLanguage(this, descriptions)
}

