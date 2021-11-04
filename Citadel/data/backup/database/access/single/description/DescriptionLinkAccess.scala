package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactoryOld
import utopia.citadel.database.model.description.DescriptionModel
import utopia.citadel.model.enumeration.CitadelDescriptionRole.Name
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.DescriptionLinkOld
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.UniqueModelAccess
import utopia.vault.nosql.view.{NonDeprecatedView, RowFactoryView, SubView}
import utopia.vault.sql.SqlExtensions._

object DescriptionLinkAccess
{
	// OTHER    -----------------------------
	
	/**
	  * @param factory A description link factory
	  * @return An access point to individual description links accessible through that factory
	  */
	def apply(factory: DescriptionLinkFactoryOld[DescriptionLinkOld]): DescriptionLinkAccessOld =
		new SimpleDescriptionLinkAccess(factory)
	
	
	// NESTED   -----------------------------
	
	private class SimpleDescriptionLinkAccess(override val factory: DescriptionLinkFactoryOld[DescriptionLinkOld])
		extends DescriptionLinkAccessOld
}

/**
  * Used for accessing individual descriptions of individual items
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
trait DescriptionLinkAccess extends SingleRowModelAccess[DescriptionLinkOld] with NonDeprecatedView[DescriptionLinkOld]
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Factory used for reading description link data
	  */
	override def factory: DescriptionLinkFactoryOld[DescriptionLinkOld]
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return model used for interacting with description links
	  */
	def linkModel = factory.model
	
	/**
	  * @return A model used for constructing description-related conditions
	  */
	protected def descriptionModel = DescriptionModel
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param id Id of the described item
	  * @return An access point to that item's description links
	  */
	def apply(id: Int) = new SingleTargetDescription(id)
	
	
	// NESTED   ----------------------------
	
	class SingleTargetDescription(targetId: Int) extends SingleRowModelAccess[DescriptionLinkOld] with SubView
	{
		// COMPUTED	------------------------
		
		/**
		  * @return An access point to this item's name description
		  */
		def name = withRole(Name)
		
		
		// IMPLEMENTED  --------------------
		
		override protected def parent = DescriptionLinkAccessOld.this
		
		override def filterCondition = linkModel.withTargetId(targetId).toCondition
		
		override def factory = parent.factory
		
		
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
		
		class DescriptionInLanguage(val languageId: Int) extends SingleRowModelAccess[DescriptionLinkOld] with SubView
		{
			// COMPUTED ---------------------
			
			/**
			  * @return An access point to name description of targeted item in this language
			  */
			def name = apply(Name)
			
			
			// IMPLEMENTED	-----------------
			
			override protected def parent = SingleTargetDescription.this
			
			override def filterCondition = descriptionModel.withLanguageId(languageId).toCondition
			
			override def factory = parent.factory
			
			
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
		
		class DescriptionOfRole(roleId: Int) extends SingleRowModelAccess[DescriptionLinkOld] with SubView
		{
			// COMPUTED -----------------------------------------
			
			/**
			  * @param connection Implicit DB Connection
			  * @param languageIds A set of accepted language ids, from most preferred to least preferred
			  * @return This description in the most preferable available language
			  */
			def inPreferredLanguage(implicit connection: Connection, languageIds: LanguageIds) =
			{
				// Reads the options in groups of three until at least one result is found
				languageIds.grouped(3).findMap { nextLanguageIds =>
					val options = find(descriptionModel.languageIdColumn.in(nextLanguageIds.toSet))
					// Selects the result that is most preferred by the user
					nextLanguageIds.findMap { languageId => options.find { _.description.languageId == languageId } }
				}
			}
			
			
			// IMPLEMENTED  -------------------------------------
			
			override protected def parent = SingleTargetDescription.this
			
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
			@deprecated("Please use inPreferredLanguage instead", "v1.3")
			def inLanguage(languageIds: Seq[Int])(implicit connection: Connection) =
				inPreferredLanguage(connection, LanguageIds(languageIds.toVector))
		}
		
		class UniqueDescriptionAccess(val languageId: Int, val roleId: Int)
			extends UniqueModelAccess[DescriptionLinkOld] with RowFactoryView[DescriptionLinkOld] with SubView
		{
			// COMPUTED --------------------------------------
			
			/**
			  * @param connection Implicit DB Connection
			  * @return Text associated with this description
			  */
			def text(implicit connection: Connection) = pullColumn(descriptionModel.textColumn).string
			
			
			// IMPLEMENTED  ----------------------------------
			
			override def factory = parent.factory
			
			override protected def parent = SingleTargetDescription.this
			
			override def filterCondition =
				descriptionModel.withLanguageId(languageId).withRoleId(roleId).toCondition
			
			
			// OTHER    -------------------------------------
			
			/**
			  * Deprecates this description
			  * @param connection Implicit DB Connection
			  * @return Whether there was a description to deprecate
			  */
			def deprecate()(implicit connection: Connection) =
				linkModel.nowDeprecated.updateWhere(condition, Some(target)) > 0
			
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
				linkModel.insert(targetId, DescriptionData(roleId, languageId, newDescription, authorId))
			}
		}
	}
}
