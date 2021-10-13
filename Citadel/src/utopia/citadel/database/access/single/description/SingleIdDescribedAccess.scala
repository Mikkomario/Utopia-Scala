package utopia.citadel.database.access.single.description

import utopia.citadel.database.access.many.description.DescriptionLinksAccess
import utopia.metropolis.model.combined.description.DescribedFactory
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.post.NewDescription
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
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
	  * @return An access point used for accessing individual description links for this model type
	  */
	protected def singleDescriptionAccess: DescriptionLinkAccess
	/**
	  * @return An access point used for accessing groups of description links for this model type
	  */
	protected def manyDescriptionsAccess: DescriptionLinksAccess
	
	/**
	 * @return Factory used for creating described compies of this model type
	 */
	protected def describedFactory: DescribedFactory[A, D]
	
	
	// COMPUTED ------------------------------
	
	/**
	 * @return An access point to the descriptions for this instance that returns multiple descriptions at once
	 */
	def descriptions = manyDescriptionsAccess(id)
	/**
	 * @return An access point to individual descriptions concerning this instance
	 */
	def description = singleDescriptionAccess(id)
	
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
	
	/**
	  * Inserts a new description for this item, replacing the existing description
	  * @param newDescription New description to apply
	  * @param connection Implicit Connection
	  * @return Inserted description link
	  */
	def describe(newDescription: DescriptionData)(implicit connection: Connection) =
		descriptions.update(newDescription)
	/**
	  * Inserts possibly multiple new descriptions for this item, replacing existing versions
	  * @param newDescription New description of this item
	  * @param authorId Id of the user who wrote the description
	  * @param connection Implicit DB connection
	  * @return Description links that were inserted
	  */
	def describe(newDescription: NewDescription, authorId: Int)(implicit connection: Connection) =
		descriptions.update(newDescription, authorId)
	
	private def pullDescribed(pullDescriptions: => Iterable[DescriptionLink])(implicit connection: Connection) =
		pull.map { item => describedFactory(item, pullDescriptions.toSet) }
}
