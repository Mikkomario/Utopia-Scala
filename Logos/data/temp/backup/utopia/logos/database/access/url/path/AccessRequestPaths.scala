package utopia.logos.database.access.url.path

import utopia.logos.database.factory.url.RequestPathDbFactory
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessRequestPaths extends AccessManyRoot[AccessRequestPaths[RequestPath]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(AccessManyRows(RequestPathDbFactory))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessRequestPaths[_]): AccessRequestPathValues = access.values
}

/**
  * Used for accessing multiple request paths from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessRequestPaths[A](wrapped: TargetingManyRows[A]) 
	extends AccessRowsWrapper[A, AccessRequestPaths[A], AccessRequestPath[A]] 
		with FilterRequestPaths[AccessRequestPaths[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible request paths
	  */
	lazy val values = AccessRequestPathValues(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessRequestPaths(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessRequestPath(target)
}

