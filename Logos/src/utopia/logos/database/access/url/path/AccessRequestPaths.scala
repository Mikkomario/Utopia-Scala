package utopia.logos.database.access.url.path

import utopia.logos.database.LogosTables
import utopia.logos.database.access.url.domain.{AccessDomainValues, FilterByDomain}
import utopia.logos.database.reader.url.{DetailedRequestPathDbReader, RequestPathDbReader}
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

object AccessRequestPaths extends AccessManyRoot[AccessRequestPathRows[RequestPath]]
{
	// ATTRIBUTES	--------------------
	
	override val root = AccessRequestPathRows(AccessManyRows(RequestPathDbReader))
	
	/**
	  * Access to request paths in the DB, also including domain information
	  */
	lazy val detailed = AccessRequestPathRows(AccessManyRows(DetailedRequestPathDbReader))
	
	
	// COMPUTED -------------------------
	
	@deprecated("Renamed to .detailed", "v0.7")
	def withDomains = detailed
}

/**
  * Used for accessing multiple request paths from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
abstract class AccessRequestPaths[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessRequestPath[A]] with FilterRequestPaths[Repr]
		with HasValues[AccessRequestPathValues]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible request paths
	  */
	lazy val values = AccessRequestPathValues(wrapped)
	
	/**
	  * A copy of this access which also targets domain
	  */
	lazy val joinedToDomains = join(LogosTables.domain)
	/**
	  * Access to the values of linked domains
	  */
	lazy val domains = AccessDomainValues(joinedToDomains)
	/**
	  * Access to domain -based filtering functions
	  */
	lazy val whereDomains = FilterByDomain(joinedToDomains)
}

/**
  * Provides access to row-specific request path -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessRequestPathRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessRequestPaths[A, AccessRequestPathRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessRequestPathRows[A], AccessRequestPath[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessRequestPathRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessRequestPath(target)
}

/**
  * Used for accessing request path items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessCombinedRequestPaths[A](wrapped: TargetingMany[A]) 
	extends AccessRequestPaths[A, AccessCombinedRequestPaths[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedRequestPaths[A], AccessRequestPath[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedRequestPaths(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessRequestPath(target)
}

