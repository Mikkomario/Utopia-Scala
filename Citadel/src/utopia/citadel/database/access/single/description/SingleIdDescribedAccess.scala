package utopia.citadel.database.access.single.description

import utopia.citadel.database.access.many.description.DbDescriptions
import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.DescriptionLinkModelFactory
import utopia.metropolis.model.combined.description.DescribedFactory
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
 * A common trait for access points that target a single model instance based on their database row id, and which
 * have a description link table associated with them (can be described)
 * @author Mikko Hilpinen
 * @since 13.10.2021, v1.3
 */
trait SingleIdDescribedAccess[A, +D] extends SingleIntIdModelAccess[A]
{
	// ABSTRACT --------------------------------
	
	/**
	 * @return Factory used for reading description links for this model type
	 */
	def descriptionLinkFactory: DescriptionLinkFactory[DescriptionLink]
	/**
	 * @return Database model (factory) user for interacting with description links for this model type
	 */
	def descriptionLinkModel: DescriptionLinkModelFactory[Storable]
	/**
	 * @return Factory used for creating described compies of this model type
	 */
	protected def describedFactory: DescribedFactory[A, D]
	
	
	// COMPUTED ------------------------------
	
	/**
	 * @return An access point to the descriptions for this instance that returns multiple descriptions at once
	 */
	def descriptions = DbDescriptions.DescriptionsOfSingle(id, descriptionLinkFactory, descriptionLinkModel)
	/**
	 * @return An access point to individual descriptions concerning this instance
	 */
	def description = DbDescription(id, descriptionLinkFactory, descriptionLinkModel)
	
	/**
	 * @param connection Implicit DB Connection
	 * @return A fully described copy of this instance which includes all available descriptions
	 *         in all available languages
	 */
	def fullyDescribed(implicit connection: Connection) = pullDescribed(descriptions)
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param languageId Id of the language in which descriptions should be read
	 * @param connection Implicit DB Connection
	 * @return A described copy of this instance, containing descriptions only in the targeted language
	 */
	def describedInLanguageWithId(languageId: Int)(implicit connection: Connection) =
		pullDescribed(descriptions.inLanguageWithId(languageId))
	/**
	 * @param languageIds Ids of the accepted languages, from most preferred to least preferred
	 * @param connection Implicit DB Connection
	 * @return A described copy of this instance, containing 0-1 descriptions per role.
	 *         Uses the most preferred available language.
	 */
	def describedInLanguages(languageIds: Seq[Int])(implicit connection: Connection) =
		pullDescribed(descriptions.inLanguages(languageIds))
	
	private def pullDescribed(pullDescriptions: => Iterable[DescriptionLink])(implicit connection: Connection) =
		pull.map { item => describedFactory(item, pullDescriptions.toSet) }
}
