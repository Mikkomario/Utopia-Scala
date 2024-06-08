package utopia.logos.database.access.many.url.request_path

import utopia.flow.collection.immutable.Empty
import utopia.logos.database.access.many.url.domain.DbDomains
import utopia.logos.database.factory.url.RequestPathDbFactory
import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.Condition

object ManyRequestPathsAccess
{
	// NESTED	--------------------
	
	private class ManyRequestPathsSubView(condition: Condition) extends ManyRequestPathsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple request paths at a time
  * @author Mikko Hilpinen
  * @since 16.10.2023, v0.1
  */
trait ManyRequestPathsAccess 
	extends ManyRequestPathsAccessLike[RequestPath, ManyRequestPathsAccess] 
		with ManyRowModelAccess[RequestPath]
{
	// COMPUTED	--------------------
	
	/**
	  * Copy of this access point that includes domain information
	  */
	def detailed = DbDetailedRequestPaths.filter(accessCondition)
	
	/**
	  * All accessible request paths, including domain information
	  * @param connection Implicit DB connection
	  */
	def pullDetailed(implicit connection: Connection) = {
		val paths = pull
		if (paths.nonEmpty) {
			// Pulls the associated domains
			val domainMap = DbDomains(paths.map { _.domainId }.toSet).toMapBy { _.id }
			// Combines the information together
			paths.map { p => DetailedRequestPath(p, domainMap(p.domainId)) }
		}
		else
			Empty
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = RequestPathDbFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyRequestPathsAccess = 
		new ManyRequestPathsAccess.ManyRequestPathsSubView(mergeCondition(filterCondition))
}

