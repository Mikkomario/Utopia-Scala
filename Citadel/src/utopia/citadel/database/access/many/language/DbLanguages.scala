package utopia.citadel.database.access.many.language

import utopia.citadel.database.access.many.description.ManyDescribedAccessByIds
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.util.EitherExtensions._
import utopia.metropolis.model.combined.language.DescribedLanguage
import utopia.metropolis.model.error.NoDataFoundException
import utopia.metropolis.model.post.NewLanguageProficiency
import utopia.metropolis.model.stored.language.{Language, LanguageFamiliarity}
import utopia.vault.database.Connection
import utopia.vault.nosql.view.UnconditionalView

import scala.util.{Failure, Success, Try}

/**
  * The root access point when targeting multiple Languages at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbLanguages extends ManyLanguagesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted Languages
	  * @return An access point to Languages with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbLanguagesSubset(ids)
	
	/**
	  * Validates the proposed language proficiencies, making sure all language ids and codes are valid
	  * @param proficiencies Proposed proficiencies
	  * @param connection    DB Connection (implicit)
	  * @return List of language -> familiarity pairs. Failure if some of the ids or codes were invalid
	  */
	def validateProposedProficiencies(proficiencies: Iterable[NewLanguageProficiency])
	                                 (implicit connection: Connection): Try[IndexedSeq[(Language, LanguageFamiliarity)]] =
	{
		if (proficiencies.isEmpty)
			Success(Empty)
		else {
			// Divides into groups so that checks can be made in bulks
			val (familiaritiesByLanguageCode, familiaritiesByLanguageId) = proficiencies
				.divideWith { p => p.language.mapBoth { _ -> p.familiarityId } { _ -> p.familiarityId } }
			// Makes sure all listed language ids are valid
			val listedLanguageIds = familiaritiesByLanguageId.map { _._1 }.toSet
			val languagesByIds = if (listedLanguageIds.isEmpty) Vector() else apply(listedLanguageIds).pull
			val missingLanguageIds = listedLanguageIds -- languagesByIds.map { _.id }
			if (missingLanguageIds.nonEmpty)
				Failure(new NoDataFoundException(s"Language ids [${missingLanguageIds.mkString(", ")}] are not valid"))
			else
			{
				// Makes sure all listed language codes are valid
				val listedLanguageCodes = familiaritiesByLanguageCode.map { _._1 }.toSet
				val languagesByIsoCodes = if (listedLanguageCodes.isEmpty) Vector() else
					withIsoCodes(listedLanguageCodes)
				val missingIsoCodes = listedLanguageCodes -- languagesByIsoCodes.map { _.isoCode }
				if (missingIsoCodes.nonEmpty)
					Failure(new NoDataFoundException(s"Language codes [${
						missingIsoCodes.mkString(", ")}] are not valid"))
				else
				{
					// Finally makes sure all the familiarity levels are valid also
					val listedFamiliarityIds = proficiencies.map { _.familiarityId }.toSet
					val familiarities = DbLanguageFamiliarities(listedFamiliarityIds).pull
					val missingFamiliarityIds = listedFamiliarityIds -- familiarities.map { _.id }
					if (missingFamiliarityIds.nonEmpty)
						Failure(new NoDataFoundException(s"Language familiarity ids [${
							missingFamiliarityIds.mkString(", ")}] are not valid"))
					else
					{
						// Combines the validated data together
						val languagesById = languagesByIds.map { l => l.id -> l }.toMap
						val languagesByIsoCode = languagesByIsoCodes.map { l => l.isoCode -> l }.toMap
						val familiaritiesById = familiarities.map { f => f.id -> f }.toMap
						Success(familiaritiesByLanguageId.map { case (languageId, familiarityId) =>
							languagesById(languageId) -> familiaritiesById(familiarityId) } ++
							familiaritiesByLanguageCode.map { case (languageCode, familiarityId) =>
								languagesByIsoCode(languageCode) -> familiaritiesById(familiarityId) })
					}
				}
			}
		}
	}
	
	
	// NESTED	--------------------
	
	class DbLanguagesSubset(override val ids: Set[Int]) 
		extends ManyLanguagesAccess with ManyDescribedAccessByIds[Language, DescribedLanguage]
}

