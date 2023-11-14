package utopia.citadel.database.access.many.description

import utopia.citadel.database.access.many.description.LinkedDescriptionsAccessLike.LinkedDescriptionsSubView
import utopia.citadel.database.factory.description.LinkedDescriptionFactory
import utopia.citadel.database.model.description.{DescriptionLinkModelFactory, DescriptionModel}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.description.LinkedDescription
import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator.LargerOrEqual
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

import java.time.Instant

object LinkedDescriptionsAccessLike
{
	// NESTED   --------------------------------
	
	private class LinkedDescriptionsSubView(override val parent: LinkedDescriptionsAccessLike,
	                                        override val filterCondition: Condition)
		extends LinkedDescriptionsAccessLike with SubView
	{
		override def factory = parent.factory
		
		override def linkModel = parent.linkModel
	}
}

/**
  * A common trait for all linked description access points that return multiple descriptions at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
trait LinkedDescriptionsAccessLike
	extends ManyRowModelAccess[LinkedDescription] with FilterableView[LinkedDescriptionsAccessLike]
{
	// ABSTRACT	-------------------------
	
	override def factory: LinkedDescriptionFactory
	/**
	  * @return A factory used for creating search models for description links
	  */
	def linkModel: DescriptionLinkModelFactory
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return A model used for constructing description-related conditions
	  */
	protected def descriptionModel = DescriptionModel
	/**
	  * @return A model factory used for constructing description search models
	  */
	protected def descriptionFactory = factory.childFactory
	
	/**
	 * @param languageIds Targeted language ids (implicit)
	 * @return An access point to description links in all of the specified languages
	 */
	def inAllPreferredLanguages(implicit languageIds: LanguageIds) =
		inLanguagesWithIds(languageIds)
	
	
	// IMPLEMENTED  --------------------
	
	override protected def self = this
	
	override def filter(additionalCondition: Condition): LinkedDescriptionsAccessLike =
		new LinkedDescriptionsSubView(this, additionalCondition)
	
	
	// OTHER	------------------------
	
	/**
	 * @param threshold  A time threshold (inclusive)
	 * @param connection DB Connection (implicit)
	 * @return Whether there are any changes to these descriptions since the specified time threshold
	 */
	def isModifiedSince(threshold: Instant)(implicit connection: Connection) =
	{
		val creationCondition = descriptionModel.withCreated(threshold).toConditionWithOperator(LargerOrEqual)
		val deletionCondition = descriptionModel.withDeprecatedAfter(threshold).toConditionWithOperator(LargerOrEqual)
		exists(creationCondition || deletionCondition)
	}
	
	/**
	 * Deprecates all description links accessible from this access point
	 * @param connection Implicit DB Connection
	 * @return Whether any row was updated
	 */
	def deprecate()(implicit connection: Connection) = putProperty(descriptionModel.deprecationAttName, Now)
	
	/**
	 * @param languageId Id of targeted language
	 * @return An access point to a subset of these descriptions. Only contains descriptions written in that language.
	 */
	def inLanguageWithId(languageId: Int) =
		filter(descriptionModel.withLanguageId(languageId).toCondition)
	/**
	 * @param languageIds Ids of the targeted languages
	 * @return An access point to description links in those languages
	 */
	def inLanguagesWithIds(languageIds: Iterable[Int]) =
		filter(descriptionModel.languageIdColumn in languageIds)
	
	/**
	 * @param roleIds    Targeted description role ids
	 * @return An access point to Recorded descriptions with those roles (in this language & target)
	 */
	def withRoleIds(roleIds: Iterable[Int]) =
		filter(descriptionModel.roleIdColumn.in(roleIds))
	/**
	 * @param roles Targeted Description roles
	 * @return An access point to Recorded descriptions with those roles (in this language & target)
	 */
	def withRoles(roles: Iterable[DescriptionRoleIdWrapper]) =
		withRoleIds(roles.map { _.id })
	/**
	 * @return An access point to Recorded descriptions with those roles (in this language & target)
	 */
	def withRoles(firstRole: DescriptionRoleIdWrapper, secondRole: DescriptionRoleIdWrapper,
	              moreRoles: DescriptionRoleIdWrapper*): LinkedDescriptionsAccessLike =
		withRoles(Set(firstRole, secondRole) ++ moreRoles)
	
	/**
	 * Accesses descriptions of this item / items, except those in excluded description roles
	 * @param excludedRoleIds Ids of the excluded description roles
	 * @return An access point to the targeted descriptions
	 */
	def outsideRoleIds(excludedRoleIds: Iterable[Int]) =
		filter(!descriptionModel.roleIdColumn.in(excludedRoleIds))
	/**
	 * Accesses descriptions of this item / items, except those in excluded description roles
	 * @param excludedRoles excluded description roles
	 * @return An access point to the targeted descriptions
	 */
	def outsideRoles(excludedRoles: Iterable[DescriptionRoleIdWrapper]) =
		outsideRoleIds(excludedRoles.map { _.id })
	
	/**
	 * @param roleId     Targeted description role's id
	 * @return Descriptions with that role for this item in targeted language
	 */
	def withRoleId(roleId: Int) = filter(descriptionModel.withRoleId(roleId).toCondition)
	/**
	 * @param role Targeted description role
	 * @return Descriptions with that role for this item in targeted language
	 */
	def withRole(role: DescriptionRoleIdWrapper) = withRoleId(role.id)
}
