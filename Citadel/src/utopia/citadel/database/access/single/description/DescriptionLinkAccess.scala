package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.{DescriptionLinkModelFactory, DescriptionModel}
import utopia.citadel.model.enumeration.CitadelDescriptionRole.Name
import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.nosql.access.single.model.{SingleModelAccess, SingleRowModelAccess}

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
		findColumn(descriptionModel.textColumn,
			descriptionModel.withLanguageId(languageId).withRoleId(roleId).toCondition).string
	/**
	  * Reads a single description text
	  * @param languageId Id of the language the description should be in
	  * @param role the role the description has
	  * @param connection Implicit DB Connection
	  * @return That description (as text) of this item
	  */
	def apply(languageId: Int, role: DescriptionRoleIdWrapper)(implicit connection: Connection): Option[String] =
		apply(languageId, role.id)
	
	/**
	  * @param languageId Id of targeted language
	  * @return An access point to a subset of these descriptions. Only contains desriptions written in that language.
	  */
	def inLanguageWithId(languageId: Int) = new DescriptionInLanguage(languageId)
	
	
	// NESTED	-------------------------
	
	class DescriptionInLanguage(val languageId: Int) extends SingleRowModelAccess[DescriptionLink]
	{
		// COMPUTED ---------------------
		
		/**
		 * @param connection Implicit DB Connection
		 * @return Name of this item in targeted language
		 */
		def name(implicit connection: Connection) = apply(Name)
		
		
		// IMPLEMENTED	-----------------
		
		override def factory = DescriptionLinkAccess.this.factory
		
		override val globalCondition = Some(DescriptionLinkAccess.this.mergeCondition(
			descriptionModel.withLanguageId(languageId).toCondition))
		
		
		// OTHER	---------------------
		
		/**
		  * @param role targeted description role
		  * @param connection Implicit connection
		  * @return Description of that role in targeted language, as text
		  */
		def apply(role: DescriptionRoleIdWrapper)(implicit connection: Connection) =
			textForRoleWithId(role.id)
		
		/**
		  * @param roleId     Targeted description role's id
		  * @param connection Db Connection
		  * @return Description for that role for this item in targeted language
		  */
		def forRoleWithId(roleId: Int)(implicit connection: Connection): Option[DescriptionLink] =
			find(descriptionModel.withRoleId(roleId).toCondition)
		
		/**
		  * @param roleId Id of the targeted description role
		  * @param connection Implicit connection
		  * @return Description of that role in targeted language, as text
		  */
		def textForRoleWithId(roleId: Int)(implicit connection: Connection) =
			findColumn(descriptionModel.textColumn, descriptionModel.withRoleId(roleId).toCondition).string
	}
}
