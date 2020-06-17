package utopia.exodus.database.access.many

import utopia.exodus.database.factory.description.DescriptionLinkFactory
import utopia.exodus.database.model.description.DescriptionModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.DescriptionRole
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyModelAccess
import utopia.vault.sql.Extensions._

/**
  * A common trait for description link access points
  * @author Mikko Hilpinen
  * @since 17.5.2020, v2
  */
trait DescriptionLinksAccess extends ManyModelAccess[DescriptionLink]
{
	// ABSTRACT	-------------------------
	
	override def factory: DescriptionLinkFactory[DescriptionLink]
	
	
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
		
		
		// OTHER	---------------------
		
		/**
		  * @param role Targeted description role
		  * @param connection Db Connection
		  * @return Description for that role for this item in targeted language
		  */
		def apply(role: DescriptionRole)(implicit connection: Connection): Option[DescriptionLink] =
			apply(Set(role)).headOption
		
		/**
		  * @param roles Targeted description roles
		  * @param connection DB Connection (implicit)
		  * @return Recorded descriptions for those roles (in this language & target)
		  */
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
		  * @param connection DB Connection (implicit)
		  * @return Read description links
		  */
		def forRolesOutside(excludedRoles: Set[DescriptionRole])(implicit connection: Connection) =
			apply(DescriptionRole.values.toSet -- excludedRoles)
	}
}
