package utopia.citadel.database.access.many.description

import utopia.citadel.database.access.many.description.DescriptionLinksAccess.DescriptionLinksSubView
import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.{DescriptionLinkModel, DescriptionLinkModelFactory, DescriptionModel}
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator.LargerOrEqual
import utopia.vault.model.immutable.Storable
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition
import utopia.vault.sql.SqlExtensions._

import java.time.Instant

object DescriptionLinksAccess
{
	// NESTED   --------------------------------
	
	private class DescriptionLinksSubView(override val parent: DescriptionLinksAccess,
	                                      override val filterCondition: Condition)
		extends DescriptionLinksAccess with SubView
	{
		override def factory = parent.factory
		
		override protected def defaultOrdering = parent.defaultOrdering
		
		override def linkModelFactory = parent.linkModelFactory
	}
}

/**
  * A common trait for all description link access points that return multiple descriptions at a time
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1.0
  */
trait DescriptionLinksAccess extends ManyRowModelAccess[DescriptionLink]
{
	// ABSTRACT	-------------------------
	
	override def factory: DescriptionLinkFactory[DescriptionLink]
	
	/**
	  * @return A factory used for creating search models for description links
	  */
	def linkModelFactory: DescriptionLinkModelFactory[Storable]
	
	
	// COMPUTED	------------------------
	
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
		val creationCondition = linkModelFactory.withCreationTime(threshold).toConditionWithOperator(LargerOrEqual)
		val deletionCondition = linkModelFactory.withDeprecatedAfter(threshold).toConditionWithOperator(LargerOrEqual)
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
	 * @param connection DB Connection (implicit)
	 * @return An access point to Recorded descriptions for those roles (in this language & target)
	 */
	def forRolesWithIds(roleIds: Set[Int])(implicit connection: Connection) =
		subView(descriptionModel.descriptionRoleIdColumn.in(roleIds))
	
	/**
	 * Accesses descriptions of this item / items, except those in excluded description roles
	 * @param excludedRoleIds Ids of the excluded description roles
	 * @param connection      DB Connection (implicit)
	 * @return An access point to the targeted descriptions
	 */
	def forRolesOutsideIds(excludedRoleIds: Set[Int])(implicit connection: Connection) =
		subView(!descriptionModel.descriptionRoleIdColumn.in(excludedRoleIds))
	
	/**
	 * @param roleId     Targeted description role's id
	 * @param connection Db Connection
	 * @return Description for that role for this item in targeted language
	 */
	def forRoleWithId(roleId: Int)(implicit connection: Connection) =
		subView(descriptionModel.withRoleId(roleId).toCondition)
	
	/**
	 * @param condition A search-condition
	 * @return A sub-view of this access point with that condition applied
	 */
	def subView(condition: Condition): DescriptionLinksAccess =
		new DescriptionLinksSubView(this, condition)
}
