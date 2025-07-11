package utopia.logos.database.access.url.path

import utopia.logos.model.stored.url.RequestPath
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessRequestPath extends AccessOneRoot[AccessRequestPath[RequestPath]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessRequestPaths.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessRequestPath[_]): AccessRequestPathValue = access.values
}

/**
  * Used for accessing individual request paths from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessRequestPath[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessRequestPath[A]] with FilterRequestPaths[AccessRequestPath[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible request path
	  */
	lazy val values = AccessRequestPathValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessRequestPath(newTarget)
}

