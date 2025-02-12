package utopia.logos.database.access.many.url.link

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.logos.database.access.many.url.path.DbRequestPaths
import utopia.logos.database.factory.url.LinkDbFactory
import utopia.logos.model.combined.url.DetailedLink
import utopia.logos.model.stored.url.StoredLink
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyLinksAccess extends ViewFactory[ManyLinksAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyLinksAccess = _ManyLinksAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _ManyLinksAccess(override val accessCondition: Option[Condition])
		 extends ManyLinksAccess
}

/**
  * A common trait for access points which target multiple links at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait ManyLinksAccess extends ManyLinksAccessLike[StoredLink, ManyLinksAccess] with ManyRowModelAccess[StoredLink]
{
	// COMPUTED	--------------------
	
	/**
	  * Pulls the accessible links as a map
	  * @param connection Implicit DB donnection
	  */
	def toMap(implicit connection: Connection) = {
		pullColumnMultiMap(model.pathId.column, model.queryParameters.column)
			.map { case (pathIdVal, paramVals) => pathIdVal.getInt -> paramVals.map { _.getModel } }
	}
	
	/**
	  * All accessible links, including request path and domain information
	  * @param connection Implicit DB connection
	  */
	def pullDetailed(implicit connection: Connection) = {
		val links = pull
		if (links.nonEmpty) {
			// Pulls associated request paths
			val pathMap = DbRequestPaths(links.map { _.pathId }.toIntSet).pullDetailed
				.view.map { p => p.id -> p }.toMap
			// Combines the links with the paths
			links.map { link => DetailedLink(link, pathMap(link.pathId)) }
		}
		else
			Empty
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LinkDbFactory
	override protected def self = this
	
	override def apply(condition: Condition): ManyLinksAccess = ManyLinksAccess(condition)
}

