package utopia.exodus.database.access.single

import utopia.exodus.database.factory.description.DescriptionLinkFactory
import utopia.exodus.database.model.description.{DescriptionLinkModelFactory, DescriptionModel}
import utopia.metropolis.model.stored.description.DescriptionLinkOld
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.nosql.access.single.model.SingleModelAccess

/**
  * A common trait for individual description link access points
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
trait DescriptionLinkAccess extends SingleModelAccess[DescriptionLinkOld]
{
	// ABSTRACT	-------------------------
	
	override def factory: DescriptionLinkFactory[DescriptionLinkOld]
	
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
	  * @param languageId Id of targeted language
	  * @return An access point to a subset of these descriptions. Only contains desriptions written in that language.
	  */
	def inLanguageWithId(languageId: Int) = DescriptionInLanguage(languageId)
	
	
	// NESTED	-------------------------
	
	case class DescriptionInLanguage(languageId: Int) extends SingleModelAccess[DescriptionLinkOld]
	{
		// IMPLEMENTED	-----------------
		
		override def factory = DescriptionLinkAccess.this.factory
		
		override val globalCondition = Some(DescriptionLinkAccess.this.mergeCondition(
			descriptionModel.withLanguageId(languageId).toCondition))
		
		
		// OTHER	---------------------
		
		/**
		  * @param roleId Targeted description role's id
		  * @param connection Db Connection
		  * @return Description for that role for this item in targeted language
		  */
		def forRoleWithId(roleId: Int)(implicit connection: Connection): Option[DescriptionLinkOld] =
			read(Some(descriptionModel.withRoleId(roleId).toCondition))
	}
}
