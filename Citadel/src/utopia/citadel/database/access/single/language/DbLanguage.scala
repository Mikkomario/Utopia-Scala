package utopia.citadel.database.access.single.language

import utopia.citadel.database.access.id.single.DbLanguageId
import utopia.citadel.database.access.many.description.DbDescriptions
import utopia.citadel.database.factory.language.LanguageFactory
import utopia.citadel.database.model.language.LanguageModel
import utopia.flow.datastructure.immutable.Pair
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.error.NoDataFoundException
import utopia.metropolis.model.post.NewLanguageProficiency
import utopia.metropolis.model.stored.language.Language
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.{SingleIntIdModelAccess, UniqueModelAccess}
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

import scala.util.{Failure, Success, Try}

/**
  * Used for accessing individual languages
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
  */
object DbLanguage extends SingleRowModelAccess[Language] with UnconditionalView with Indexed
{
	// COMPUTED ------------------------------------
	
	private def model = LanguageModel
	
	
	// IMPLEMENTED	--------------------------------
	
	override def factory = LanguageFactory
	
	
	// OTHER	------------------------------------
	
	/**
	 * @param id A language id
	 * @return An access point to that language
	 */
	def apply(id: Int) = new DbSingleLanguage(id)
	
	/**
	 * @param languageCode A language code
	 * @param connection Implicit DB Connection
	 * @return An access point to a language with that ISO-code
	 */
	def forIsoCode(languageCode: String)(implicit connection: Connection) = new DbLanguageForIsoCode(languageCode)
	
	/**
	  * Validates the proposed language proficiencies, making sure all language ids and codes are valid
	  * @param proficiencies Proposed proficiencies
	  * @param connection    DB Connection (implicit)
	  * @return List of language id -> familiarity id pairs. Failure if some of the ids or codes were invalid
	  */
	def validateProposedProficiencies(proficiencies: Vector[NewLanguageProficiency])
	                                 (implicit connection: Connection): Try[Vector[Pair[Int]]] =
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
			languageId.flatMap[Pair[Int]] { languageId =>
				// Validates language familiarity id
				if (DbLanguageFamiliarity(proficiency.familiarityId).isDefined)
					Success(languageId -> proficiency.familiarityId)
				else
					Failure(new NoDataFoundException(s"${ proficiency.familiarityId } is not a valid language familiarity id"))
			}
		}
	}
	
	
	// NESTED   -----------------------------------
	
	class DbSingleLanguage(override val id: Int) extends SingleIntIdModelAccess[Language]
	{
		override def factory = DbLanguage.factory
		
		/**
		 * @param connection Implicit DB Connection
		 * @return The ISO-code associated with this language
		 */
		def isoCode(implicit connection: Connection) = pullAttribute(model.isoCodeAttName)
		
		/**
		 * @return An access point to this language's descriptions
		 */
		def descriptions = DbDescriptions.ofLanguageWithId(id)
	}
	
	class DbLanguageForIsoCode(val code: String) extends UniqueModelAccess[Language] with SubView
	{
		// COMPUTED -------------------------------
		
		/**
		 * @param connection Implicit DB Connection
		 * @return Id of this language, if found
		 */
		def id(implicit connection: Connection) = pullColumn(index).int
		
		
		// IMPLEMENTED  ---------------------------
		
		override protected def parent = DbLanguage
		
		override def filterCondition = model.withIsoCode(code).toCondition
		
		override def factory = parent.factory
		
		
		// OTHER    --------------------------------
		
		/**
		 * Reads this language, inserting one if it doesn't exist already
		 * @param connection Implicit DB Connection
		 * @return Read or inserted language
		 */
		def getOrInsert()(implicit connection: Connection) = pull.getOrElse { model.insert(code) }
	}
}
