package utopia.logos.database.access.many.url.link

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.LogosContext
import utopia.logos.database.storable.url.LinkDbModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

import java.time.Instant

/**
  * A common trait for access points which target multiple links or similar instances at a time
  * @tparam A Type of read (links -like) instances
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait ManyLinksAccessLike[+A, +Repr] extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * path ids of the accessible links
	  */
	def pathIds(implicit connection: Connection) = pullColumn(model.pathId.column).map { v => v.getInt }
	/**
	  * query parameters of the accessible links
	  */
	def queryParameters(implicit connection: Connection) = 
		pullColumn(model.queryParameters.column).map { v => v.notEmpty match {
			 case Some(v) => LogosContext.jsonParser.valueOf(v.getString).getModel; case None => Model.empty } }
	/**
	  * creation times of the accessible links
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.created.column).map { v => v.getInstant }
	
	/**
	  * Unique ids of the accessible links
	  */
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model = LinkDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted links
	  * @param newCreated A new created to assign
	  * @return Whether any link was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created.column, newCreated)
	/**
	  * Updates the query parameterses of the targeted links
	  * @param newQueryParameters A new query parameters to assign
	  * @return Whether any link was affected
	  */
	def queryParameters_=(newQueryParameters: Model)(implicit connection: Connection) = 
		putColumn(model.queryParameters.column, newQueryParameters.notEmpty.map { _.toJson })
	
	/**
	  * @param pathId path id to target
	  * @return Copy of this access point that only includes links with the specified path id
	  */
	def withPath(pathId: Int) = filter(model.pathId.column <=> pathId)
	/**
	  * @param pathIds Targeted path ids
	  * @return Copy of this access point that only includes links where path id is within the specified value set
	  */
	def withPaths(pathIds: IterableOnce[Int]) = filter(model.pathId.column.in(IntSet.from(pathIds)))
}

