package utopia.citadel.database.access.many.description

import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.description.{DescribedFactory, LinkedDescription}
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed

/**
 * A common trait for access points that return multiple instances of models that support descriptions
 * @author Mikko Hilpinen
 * @since 13.10.2021, v1.3
 */
trait ManyDescribedAccess[A, +D] extends ManyModelAccess[A] with Indexed
{
	// ABSTRACT --------------------------------
	
	/**
	  * @return An access point that provides access to this model type's descriptions
	  */
	protected def manyDescriptionsAccess: LinkedDescriptionsAccess
	/**
	 * @return Factory used for creating described compies of this model type
	 */
	protected def describedFactory: DescribedFactory[A, D]
	
	/**
	  * @param item An item
	  * @return The row id of that item
	  */
	protected def idOf(item: A): Int
	
	
	// COMPUTED --------------------------------
	
	/**
	  * @param connection Implicit DB Connection
	  * @return All accessible items with all available descriptions
	  */
	def fullyDescribed(implicit connection: Connection) = pullDescribed { _.pull.groupBy { _.targetId } }
	/**
	  * @param connection Implicit DB connection
	  * @param languageIds Ids of the targeted languages, from most to least preferred
	  * @return All accessible items described. Descriptions are chosen from the specified languages. Only one
	  *         description per description role is included.
	  */
	def described(implicit connection: Connection, languageIds: LanguageIds) =
	{
		if (languageIds.isEmpty)
			fullyDescribed
		else
			pullDescribed { _.inPreferredLanguages }
	}
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param languageIds Ids of the targeted languages, from most to least preferred
	  * @param connection Implicit DB connection
	  * @return All accessible items described. Descriptions are chosen from the specified languages. Only one
	  *         description per description role is included.
	  */
	@deprecated("Please use described instead", "v1.3")
	def describedInLanguages(languageIds: Seq[Int])(implicit connection: Connection) =
		described(connection, LanguageIds(languageIds.toVector))
	
	/**
	  * @param languageId Id of the target language
	  * @param connection Implicit DB Connection
	  * @return Described versions of all accessible items where the descriptions are in the specified language
	  */
	def describedInLanguageWithId(languageId: Int)(implicit connection: Connection) =
		pullDescribed { _.inLanguageWithId(languageId).pull.groupBy { _.targetId } }
	
	private def pullDescribed(pullDescriptions: LinkedDescriptionsAccess#DescriptionsOfMany => Map[Int, Iterable[LinkedDescription]])
	                         (implicit connection: Connection) =
	{
		val items = pull
		val descriptions = pullDescriptions(manyDescriptionsAccess(items.map(idOf)))
		items.map { item => describedFactory(item, descriptions.getOrElse(idOf(item), Vector()).toSet) }
	}
}
