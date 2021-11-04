package utopia.citadel.database.access.single.description

import utopia.citadel.database.access.many.description.LinkedDescriptionsAccess
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.description.{DescribedFactory, LinkedDescription}
import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.post.NewDescription
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
	protected def singleDescriptionAccess: LinkedDescriptionAccess
	/**
	  * @return An access point used for accessing groups of description links for this model type
	  */
	protected def manyDescriptionsAccess: LinkedDescriptionsAccess
	
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
	def fullyDescribed(implicit connection: Connection) = pullDescribed { descriptions }
	/**
	  * @param connection Implicit DB Connection
	  * @param languageIds Ids of the accepted languages, from most preferred to least preferred
	  * @return A described copy of this instance, containing 0-1 descriptions per role.
	  *         Uses the most preferred available language.
	  */
	def described(implicit connection: Connection, languageIds: LanguageIds) =
		pullDescribed { descriptions.inPreferredLanguages }
	
	
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
	@deprecated("Please use described instead", "v1.3")
	def describedInLanguages(languageIds: Seq[Int])(implicit connection: Connection) =
		described(connection, LanguageIds(languageIds.toVector))
	
	/**
	 * @param roleIds Ids of the targeted description roles
	 * @param connection Implicit DB Connection
	 * @param languageIds Accepted language ids
	 * @return A described copy of this instance, containing 0-1 descriptions per role.
	 */
	def describedWithRoleIds(roleIds: Set[Int])(implicit connection: Connection, languageIds: LanguageIds) =
		pullDescribed { descriptions.withRolesInPreferredLanguages(roleIds) }
	/**
	 * @param roles Targeted description roles
	 * @param connection Implicit DB Connection
	 * @param languageIds Accepted language ids
	 * @return A described copy of this instance, containing 0-1 descriptions per role.
	 */
	def describedWith(roles: Set[DescriptionRoleIdWrapper])
	                 (implicit connection: Connection, languageIds: LanguageIds) =
		describedWithRoleIds(roles.map { _.id })
	/**
	 * @param connection Implicit DB Connection
	 * @param languageIds Accepted language ids
	 * @return A described copy of this instance, containing 0-1 descriptions per role.
	 */
	def describedWith(firstRole: DescriptionRoleIdWrapper, secondRole: DescriptionRoleIdWrapper,
	                  moreRoles: DescriptionRoleIdWrapper*)
	                 (implicit connection: Connection, languageIds: LanguageIds): Option[D] =
		describedWith(Set(firstRole, secondRole) ++ moreRoles)
	
	/**
	 * @param role Targeted description role
	 * @param connection Implicit DB Connection
	 * @param languageIds Accepted language ids
	 * @return A described copy of this instance, containing a description for the specified role, if available
	 */
	def withDescription(role: DescriptionRoleIdWrapper)(implicit connection: Connection, languageIds: LanguageIds) =
		pullDescribed { description.withRole(role).inPreferredLanguage }
	
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
	
	private def pullDescribed(pullDescriptions: => Iterable[LinkedDescription])(implicit connection: Connection) =
		pull.map { item => describedFactory(item, pullDescriptions.toSet) }
}
