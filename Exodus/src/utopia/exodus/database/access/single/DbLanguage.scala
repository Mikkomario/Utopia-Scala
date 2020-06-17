package utopia.exodus.database.access.single

import utopia.exodus.database.access.id.LanguageId
import utopia.exodus.database.factory.language.LanguageFactory
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.error.NoDataFoundException
import utopia.metropolis.model.post.NewLanguageProficiency
import utopia.metropolis.model.stored.language.Language
import utopia.vault.database.Connection
import utopia.vault.nosql.access.SingleModelAccessById

import scala.util.{Failure, Success}

/**
  * Used for accessing individual languages
  * @author Mikko Hilpinen
  * @since 2.5.2020, v2
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
	  * @param connection DB Connection (implicit)
	  * @return List of language id -> familiarity pairs. Failure if some of the ids or codes were invalid
	  */
	def validateProposedProficiencies(proficiencies: Vector[NewLanguageProficiency])(implicit connection: Connection) =
	{
		proficiencies.tryMap { proficiency =>
			val languageId = proficiency.language match
			{
				case Right(languageId) =>
					if (apply(languageId).isDefined)
						Success(languageId)
					else
						Failure(new NoDataFoundException(s"$languageId is not a valid language id"))
				case Left(languageCode) =>
					LanguageId.forIsoCode(languageCode).pull.toTry {
						new NoDataFoundException(s"$languageCode is not a valid language code") }
			}
			languageId.map { _ -> proficiency.familiarity }
		}
	}
}
