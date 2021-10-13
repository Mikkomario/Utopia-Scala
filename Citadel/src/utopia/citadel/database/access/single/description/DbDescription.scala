package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.{DescriptionLinkModel, DescriptionLinkModelFactory, DescriptionModel}
import utopia.citadel.model.enumeration.CitadelDescriptionRole.Name
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.UniqueModelAccess
import utopia.vault.nosql.view.{RowFactoryView, SubView}
import utopia.vault.sql.SqlExtensions._

object DbDescription
{
	/**
	 * @param organizationId Organization id
	 * @return An access point to that organization's individual descriptions
	 */
	def ofOrganizationWithId(organizationId: Int) =
		apply(organizationId, DescriptionLinkFactory.organization, DescriptionLinkModel.organization)
	
	/**
	 * @param deviceId Device id
	 * @return An access point to that device's individual descriptions
	 */
	def ofDeviceWithId(deviceId: Int) =
		apply(deviceId, DescriptionLinkFactory.device, DescriptionLinkModel.device)
	
	/**
	 * @param taskId Task id
	 * @return An access point to individual descriptions of that task type
	 */
	def ofTaskWithId(taskId: Int) = apply(taskId, DescriptionLinkFactory.task,
		DescriptionLinkModel.task)
	
	/**
	 * @param roleId User role id
	 * @return An access point to individual descriptions of that user role
	 */
	def ofUserRoleWithId(roleId: Int) =
		apply(roleId, DescriptionLinkFactory.userRole, DescriptionLinkModel.userRole)
	
	/**
	 * @param languageId Language id
	 * @return An access point to that language's individual descriptions
	 */
	def ofLanguageWithId(languageId: Int) =
		apply(languageId, DescriptionLinkFactory.language, DescriptionLinkModel.language)
	
	/**
	 * @param roleId Id of the targeted description role
	 * @return An access point to that description role's individual descriptions
	 */
	def ofDescriptionRoleWithId(roleId: Int) =
		apply(roleId, DescriptionLinkFactory.descriptionRole, DescriptionLinkModel.descriptionRole)
	/**
	  * @param descriptionRole A description role
	  * @return An access point to that description role's individual descriptions
	  */
	def of(descriptionRole: DescriptionRoleIdWrapper) = ofDescriptionRoleWithId(descriptionRole.id)
}

/**
 * Used for accessing individual descriptions of individual items (based on their row id)
 * @author Mikko Hilpinen
 * @since 13.10.2021, v1.3
 */
case class DbDescription(targetId: Int, factory: DescriptionLinkFactory[DescriptionLink],
                         linkModelFactory: DescriptionLinkModelFactory[Storable])
	extends SingleRowModelAccess[DescriptionLink]
{
	// COMPUTED	------------------------
	
	/**
	 * @return A model used for constructing description-related conditions
	 */
	private def descriptionModel = DescriptionModel
	
	/**
	 * @return An access point to this item's name description
	 */
	def name = withRole(Name)
	
	
	// IMPLEMENTED  --------------------
	
	override def globalCondition = Some(linkModelFactory.withTargetId(targetId).toCondition &&
		factory.nonDeprecatedCondition)
	
	
	// OTHER	------------------------
	
	/**
	 * @param roleId Id of the targeted description role
	 * @return An access point to that role's individual descriptions
	 */
	def withRoleId(roleId: Int) = new DescriptionOfRole(roleId)
	/**
	 * @param role targeted description role
	 * @return An access point to that role's individual descriptions
	 */
	def withRole(role: DescriptionRoleIdWrapper) = withRoleId(role.id)
	
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
		 * @return An access point to name description of targeted item in this language
		 */
		def name = apply(Name)
		
		
		// IMPLEMENTED	-----------------
		
		override def factory = DbDescription.this.factory
		
		override val globalCondition = Some(DbDescription.this.mergeCondition(
			descriptionModel.withLanguageId(languageId).toCondition))
		
		
		// OTHER	---------------------
		
		/**
		 * @param roleId     Targeted description role's id
		 * @return An access point to that description
		 */
		def withRoleId(roleId: Int) = new UniqueDescriptionAccess(languageId, roleId)
		/**
		 * @param role A description role
		 * @return An access point to that description
		 */
		def apply(role: DescriptionRoleIdWrapper) = withRoleId(role.id)
	}
	
	class DescriptionOfRole(roleId: Int) extends SingleRowModelAccess[DescriptionLink] with SubView
	{
		// IMPLEMENTED  -------------------------------------
		
		override protected def parent = DbDescription.this
		
		override def filterCondition = descriptionModel.withRoleId(roleId).toCondition
		
		override def factory = parent.factory
		
		
		// OTHER    -----------------------------------------
		
		/**
		 * @param languageId Id of the targeted language
		 * @return An access point to that description
		 */
		def inLanguageWithId(languageId: Int) = new UniqueDescriptionAccess(languageId, roleId)
		
		/**
		 * @param languageIds A set of accepted language ids, from most preferred to least preferred
		 * @param connection Implicit DB Connection
		 * @return This description in the most preferable available language
		 */
		def inLanguage(languageIds: Seq[Int])(implicit connection: Connection) =
		{
			// Reads the options in groups of three until at least one result is found
			languageIds.grouped(3).findMap { nextLanguageIds =>
				val options = find(descriptionModel.languageIdColumn.in(nextLanguageIds.toSet))
				// Selects the result that is most preferred by the user
				nextLanguageIds.findMap { languageId => options.find { _.description.languageId == languageId } }
			}
		}
	}
	
	class UniqueDescriptionAccess(val languageId: Int, val roleId: Int)
		extends UniqueModelAccess[DescriptionLink] with RowFactoryView[DescriptionLink] with SubView
	{
		// COMPUTED --------------------------------------
		
		/**
		 * @param connection Implicit DB Connection
		 * @return Text associated with this description
		 */
		def text(implicit connection: Connection) = pullColumn(descriptionModel.textColumn).string
		
		
		// IMPLEMENTED  ----------------------------------
		
		override def factory = parent.factory
		
		override protected def parent = DbDescription.this
		
		override def filterCondition =
			descriptionModel.withLanguageId(languageId).withRoleId(roleId).toCondition
		
		
		// OTHER    -------------------------------------
		
		/**
		 * Deprecates this description
		 * @param connection Implicit DB Connection
		 * @return Whether there was a description to deprecate
		 */
		def deprecate()(implicit connection: Connection) =
			linkModelFactory.nowDeprecated.updateWhere(condition, Some(target)) > 0
		
		/**
		 * Replaces this description with a new version
		 * @param newDescription New description of this item as text
		 * @param authorId Id of the user who wrote this description, if applicable (optional)
		 * @param connection Implicit DB Connection
		 * @return Inserted description link
		 */
		def replaceWith(newDescription: String, authorId: Option[Int] = None)(implicit connection: Connection) =
		{
			deprecate()
			linkModelFactory.insert(targetId, DescriptionData(roleId, languageId, newDescription, authorId))
		}
	}
}
