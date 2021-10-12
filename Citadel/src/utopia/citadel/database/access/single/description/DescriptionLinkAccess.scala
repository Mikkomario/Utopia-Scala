package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.{DescriptionLinkModelFactory, DescriptionModel}
import utopia.citadel.model.enumeration.StandardDescriptionRoleId
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.sql.{Select, Where}

/**
  * A common trait for individual description link access points
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1.0
  */
trait DescriptionLinkAccess extends SingleModelAccess[DescriptionLink]
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
	 * Reads a single description text
	 * @param languageId Id of the language the description should be in
	 * @param roleId Id of the role the description has
	 * @param connection Implicit DB Connection
	 * @return That description (as text) of this item
	 */
	def apply(languageId: Int, roleId: Int)(implicit connection: Connection) =
		connection(Select(target, descriptionModel.textColumn) +
			Where(mergeCondition(descriptionModel.withLanguageId(languageId).withRoleId(roleId))))
			.firstValue.string
	
	/**
	  * @param languageId Id of targeted language
	  * @return An access point to a subset of these descriptions. Only contains desriptions written in that language.
	  */
	def inLanguageWithId(languageId: Int) = new DescriptionInLanguage(languageId)
	
	
	// NESTED	-------------------------
	
	class DescriptionInLanguage(val languageId: Int) extends SingleModelAccess[DescriptionLink]
	{
		// COMPUTED ---------------------
		
		/**
		 * @param connection Implicit DB Connection
		 * @return Link to the name description of this item
		 */
		def name(implicit connection: Connection) =
			forRoleWithId(StandardDescriptionRoleId.name)
		
		
		// IMPLEMENTED	-----------------
		
		override def factory = DescriptionLinkAccess.this.factory
		
		override val globalCondition = Some(DescriptionLinkAccess.this.mergeCondition(
			descriptionModel.withLanguageId(languageId).toCondition))
		
		
		// OTHER	---------------------
		
		/**
		  * @param roleId     Targeted description role's id
		  * @param connection Db Connection
		  * @return Description for that role for this item in targeted language
		  */
		def forRoleWithId(roleId: Int)(implicit connection: Connection): Option[DescriptionLink] =
			read(Some(descriptionModel.withRoleId(roleId).toCondition))
	}
}
