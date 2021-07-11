package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.{DescriptionLinkModel, DescriptionLinkModelFactory, DescriptionModel}
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.DescriptionRole
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator.LargerOrEqual
import utopia.vault.model.immutable.Storable
import utopia.vault.nosql.access.many.model.{ManyModelAccess, ManyRowModelAccess}
import utopia.vault.sql.Condition
import utopia.vault.sql.SqlExtensions._

import java.time.Instant

/**
  * A common trait for description link access points
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1.0
  */
trait DescriptionLinksAccess extends ManyModelAccess[DescriptionLink]
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
	  * @param languageId Id of targeted language
	  * @return An access point to a subset of these descriptions. Only contains desriptions written in that language.
	  */
	def inLanguageWithId(languageId: Int) = DescriptionsInLanguage(languageId)
	
	
	// NESTED	-------------------------
	
	case class DescriptionsInLanguage(languageId: Int) extends ManyModelAccess[DescriptionLink]
	{
		// IMPLEMENTED	-----------------
		
		override def factory = DescriptionLinksAccess.this.factory
		
		override val globalCondition = Some(DescriptionLinksAccess.this.mergeCondition(
			descriptionModel.withLanguageId(languageId).toCondition))
		
		override protected def defaultOrdering = None
		
		
		// OTHER	---------------------
		
		/**
		  * @param roleIds    Targeted description role ids
		  * @param connection DB Connection (implicit)
		  * @return An access point to Recorded descriptions for those roles (in this language & target)
		  */
		def forRolesWithIds(roleIds: Set[Int])(implicit connection: Connection) =
			new DbDescriptionsOfRole(descriptionModel.descriptionRoleIdColumn.in(roleIds))
		
		/**
		  * Accesses descriptions of this item / items, except those in excluded description roles
		  * @param excludedRoleIds Ids of the excluded description roles
		  * @param connection      DB Connection (implicit)
		  * @return An access point to the targeted descriptions
		  */
		def forRolesOutsideIds(excludedRoleIds: Set[Int])(implicit connection: Connection) =
			new DbDescriptionsOfRole(!descriptionModel.descriptionRoleIdColumn.in(excludedRoleIds))
		
		/**
		  * @param roleId     Targeted description role's id
		  * @param connection Db Connection
		  * @return Description for that role for this item in targeted language
		  */
		def forRoleWithId(roleId: Int)(implicit connection: Connection) =
			new DbDescriptionsOfRole(descriptionModel.withRoleId(roleId).toCondition)
		
		/**
		  * @param role       Targeted description role
		  * @param connection Db Connection
		  * @return Description for that role for this item in targeted language
		  */
		@deprecated("Replaced with .forRoleWithId(Int)", "v1.0")
		def apply(role: DescriptionRole)(implicit connection: Connection): Option[DescriptionLink] =
			apply(Set(role)).headOption
		
		/**
		  * @param roles      Targeted description roles
		  * @param connection DB Connection (implicit)
		  * @return Recorded descriptions for those roles (in this language & target)
		  */
		@deprecated("Replaced with .forRolesWithIds(...)", "v1.0")
		def apply(roles: Set[DescriptionRole])(implicit connection: Connection) =
		{
			if (roles.nonEmpty)
				read(Some(descriptionModel.descriptionRoleIdColumn.in(roles.map { _.id })))
			else
				Vector()
		}
		
		/**
		  * Reads descriptions of this item, except those in excluded description roles
		  * @param excludedRoles Excluded description roles
		  * @param connection    DB Connection (implicit)
		  * @return Read description links
		  */
		@deprecated("Replaced with .forRolesOutsideIds(...)", "v1.0")
		def forRolesOutside(excludedRoles: Set[DescriptionRole])(implicit connection: Connection) =
			apply(DescriptionRole.values.toSet -- excludedRoles)
		
		// NESTED   -----------------------------------
		
		class DbDescriptionsOfRole(roleCondition: Condition) extends ManyRowModelAccess[DescriptionLink]
		{
			// ATTRIBUTES   ----------------------------
			
			override val globalCondition = Some(DescriptionsInLanguage.this.mergeCondition(roleCondition))
			
			
			// IMPLEMENTED  ----------------------------
			
			override def factory = DescriptionsInLanguage.this.factory
			
			override protected def defaultOrdering = None
			
			
			// OTHER    --------------------------------
			
			/**
			  * Deprecates all description links accessible from this access point
			  * @param connection Implicit DB Connection
			  * @return Whether any row was updated
			  */
			def deprecate()(implicit connection: Connection) =
				putAttribute(DescriptionLinkModel.deprecationAttName, Now)
		}
	}
}
