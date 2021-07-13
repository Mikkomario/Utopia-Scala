package utopia.citadel.database.access.single.language

import utopia.citadel.database.access.id.single.DbLanguageId
import utopia.citadel.database.factory.language.LanguageFactory
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.error.NoDataFoundException
import utopia.metropolis.model.post.NewLanguageProficiency
import utopia.metropolis.model.stored.language.Language
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccessById

import scala.util.{Failure, Success}

/**
  * Used for accessing individual languages
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
  */
object DbLanguage extends SingleModelAccessById[Language, Int]
{
	// IMPLEMENTED	--------------------------------
	
	override def idToValue(id: Int) = id
	
	override def factory = LanguageFactory
	
	
	// OTHER	------------------------------------
	
	/**
	  * Validates the proposed language proficiencies, making sure all language ids and codes are valid
	  * @param proficiencies Proposed proficiencies
	  * @param connection    DB Connection (implicit)
	  * @return List of language id -> familiarity id pairs. Failure if some of the ids or codes were invalid
	  */
	def validateProposedProficiencies(proficiencies: Vector[NewLanguageProficiency])(implicit connection: Connection) =
	{
		proficiencies.tryMap { proficiency =>
			// Validates / retrieves language id
			val languageId = proficiency.language match {
				case Right(languageId) =>
					if (apply(languageId).isDefined)
						Success(languageId)
					else
						Failure(new NoDataFoundException(s"$languageId is not a valid language id"))
				case Left(languageCode) =>
					DbLanguageId.forIsoCode(languageCode).pull.toTry {
						new NoDataFoundException(s"$languageCode is not a valid language code")
					}
			}
			languageId.flatMap { languageId =>
				// Validates language familiarity id
				if (DbLanguageFamiliarity(proficiency.familiarityId).isDefined)
					Success(languageId -> proficiency.familiarityId)
				else
					Failure(new NoDataFoundException(s"${ proficiency.familiarityId } is not a valid language familiarity id"))
			}
		}
	}
}
