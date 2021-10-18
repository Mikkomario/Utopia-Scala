package utopia.citadel.database.access.many.description

import utopia.citadel.database.access.many.description.DescriptionLinksAccessLike.DescriptionLinksSubView
import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.{DescriptionLinkModel, DescriptionModel}
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator.LargerOrEqual
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition
import utopia.vault.sql.SqlExtensions._

import java.time.Instant

object DescriptionLinksAccessLike
{
	// NESTED   --------------------------------
	
	private class DescriptionLinksSubView(override val parent: DescriptionLinksAccessLike,
	                                      override val filterCondition: Condition)
		extends DescriptionLinksAccessLike with SubView
	{
		override def factory = parent.factory
		
		override protected def defaultOrdering = parent.defaultOrdering
		
		override def linkModel = parent.linkModel
	}
}

/**
  * A common trait for all description link access points that return multiple descriptions at a time
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1.0
  */
trait DescriptionLinksAccessLike extends ManyRowModelAccess[DescriptionLink]
{
	// ABSTRACT	-------------------------
	
	override def factory: DescriptionLinkFactory[DescriptionLink]
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return A factory used for creating search models for description links
	  */
	def linkModel = factory.model
	
	/**
	  * @return A model used for constructing description-related conditions
	  */
	protected def descriptionModel = DescriptionModel
	/**
	  * @return A model factory used for constructing description search models
	  */
	protected def descriptionFactory = factory.childFactory
	
	
	// IMPLEMENTED  --------------------
	
	override protected def defaultOrdering = None
	
	
	// OTHER	------------------------
	
	/**
	 * @param threshold  A time threshold (inclusive)
	 * @param connection DB Connection (implicit)
	 * @return Whether there are any changes to these descriptions since the specified time threshold
	 */
	def isModifiedSince(threshold: Instant)(implicit connection: Connection) =
	{
		val creationCondition = linkModel.withCreationTime(threshold).toConditionWithOperator(LargerOrEqual)
		val deletionCondition = linkModel.withDeprecatedAfter(threshold).toConditionWithOperator(LargerOrEqual)
		exists(creationCondition || deletionCondition)
	}
	
	/**
	 * Deprecates all description links accessible from this access point
	 * @param connection Implicit DB Connection
	 * @return Whether any row was updated
	 */
	def deprecate()(implicit connection: Connection) =
		putAttribute(DescriptionLinkModel.deprecationAttName, Now)
	
	/**
	 * @param languageId Id of targeted language
	 * @return An access point to a subset of these descriptions. Only contains descriptions written in that language.
	 */
	def inLanguageWithId(languageId: Int) =
		subView(descriptionModel.withLanguageId(languageId).toCondition)
	
	/**
	 * @param roleIds    Targeted description role ids
	 * @return An access point to Recorded descriptions with those roles (in this language & target)
	 */
	def withRoleIds(roleIds: Iterable[Int]) =
		subView(descriptionModel.roleIdColumn.in(roleIds))
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
	              moreRoles: DescriptionRoleIdWrapper*): DescriptionLinksAccessLike =
		withRoles(Set(firstRole, secondRole) ++ moreRoles)
	/**
	 * @param roleIds    Targeted description role ids
	 * @return An access point to Recorded descriptions for those roles (in this language & target)
	 */
	@deprecated("Please use withRoleIds instead", "v1.3")
	def forRolesWithIds(roleIds: Iterable[Int]) = withRoleIds(roleIds)
	/**
	  * @param roles Targeted Description roles
	  * @return An access point to Recorded descriptions for those roles (in this language & target)
	  */
	@deprecated("Please use withRoles instead", "v1.3")
	def forRoles(roles: Iterable[DescriptionRoleIdWrapper]) = withRoles(roles)
	
	/**
	 * Accesses descriptions of this item / items, except those in excluded description roles
	 * @param excludedRoleIds Ids of the excluded description roles
	 * @return An access point to the targeted descriptions
	 */
	def outsideRoleIds(excludedRoleIds: Iterable[Int]) =
		subView(!descriptionModel.roleIdColumn.in(excludedRoleIds))
	/**
	 * Accesses descriptions of this item / items, except those in excluded description roles
	 * @param excludedRoles excluded description roles
	 * @return An access point to the targeted descriptions
	 */
	def outsideRoles(excludedRoles: Iterable[DescriptionRoleIdWrapper]) =
		outsideRoleIds(excludedRoles.map { _.id })
	/**
	 * Accesses descriptions of this item / items, except those in excluded description roles
	 * @param excludedRoleIds Ids of the excluded description roles
	 * @return An access point to the targeted descriptions
	 */
	@deprecated("Please use outsideRoleIds instead", "v1.3")
	def forRolesOutsideIds(excludedRoleIds: Iterable[Int]) = outsideRoleIds(excludedRoleIds)
	/**
	  * Accesses descriptions of this item / items, except those in excluded description roles
	  * @param excludedRoles excluded description roles
	  * @return An access point to the targeted descriptions
	  */
	@deprecated("Please use outsideRoles instead", "v1.3")
	def forRolesOutside(excludedRoles: Iterable[DescriptionRoleIdWrapper]) =
		outsideRoles(excludedRoles)
	
	/**
	 * @param roleId     Targeted description role's id
	 * @return Descriptions with that role for this item in targeted language
	 */
	def withRoleId(roleId: Int) = subView(descriptionModel.withRoleId(roleId).toCondition)
	/**
	 * @param role Targeted description role
	 * @return Descriptions with that role for this item in targeted language
	 */
	def withRole(role: DescriptionRoleIdWrapper) = withRoleId(role.id)
	/**
	 * @param roleId     Targeted description role's id
	 * @return Description for that role for this item in targeted language
	 */
	@deprecated("Please use withRoleId instead", "v1.3")
	def forRoleWithId(roleId: Int) = withRoleId(roleId)
	/**
	  * @param role Targeted description role
	  * @return Description for that role for this item in targeted language
	  */
	@deprecated("Please use WithRole instead", "v1.3")
	def forRole(role: DescriptionRoleIdWrapper) = withRole(role)
	
	/**
	 * @param condition A search-condition
	 * @return A sub-view of this access point with that condition applied
	 */
	def subView(condition: Condition): DescriptionLinksAccessLike =
		new DescriptionLinksSubView(this, condition)
}
