package utopia.logos.database.access.many.url.link

import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.Condition
import utopia.logos.database.access.many.url.request_path.DbRequestPaths
import utopia.logos.database.factory.url.LinkFactory
import utopia.logos.model.combined.url.DetailedLink
import utopia.logos.model.stored.url.Link

object ManyLinksAccess
{
	// NESTED	--------------------
	
	private class ManyLinksSubView(condition: Condition) extends ManyLinksAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple links at a time
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
trait ManyLinksAccess extends ManyLinksAccessLike[Link, ManyLinksAccess] with ManyRowModelAccess[Link]
{
	// COMPUTED ------------------------
	
	/**
	 * Pulls the accessible links as a map
	 * @param connection Implicit DB donnection
	 * @return Accessible links as a map where keys are request path ids and values are assigned parameter models.
	 *         One model is provided for each link.
	 */
	def toMap(implicit connection: Connection) =
		pullColumnMultiMap(model.requestPathIdColumn, model.queryParametersColumn)
			.map { case (pathIdVal, paramVals) => pathIdVal.getInt -> paramVals.map { _.getModel } }
	
	/**
	 * @param connection Implicit DB connection
	 * @return All accessible links, including request path and domain information
	 */
	def pullDetailed(implicit connection: Connection) = {
		val links = pull
		if (links.nonEmpty) {
			// Pulls associated request paths
			val pathMap = DbRequestPaths(links.map { _.requestPathId }.toSet).pullDetailed
				.view.map { p => p.id -> p }.toMap
			// Combines the links with the paths
			links.map { link => DetailedLink(link, pathMap(link.requestPathId)) }
		}
		else
			Vector()
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LinkFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyLinksAccess = 
		new ManyLinksAccess.ManyLinksSubView(mergeCondition(filterCondition))
}

