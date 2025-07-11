package utopia.logos.database.access.url.path

import utopia.logos.database.LogosTables
import utopia.logos.database.access.url.domain.{AccessDomainValue, FilterByDomain}
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessRequestPath extends AccessOneRoot[AccessRequestPath[RequestPath]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessRequestPaths.root.head
	
	/**
	  * Access to individual request paths in the DB, also including domain information
	  */
	lazy val withDomain = AccessRequestPaths.withDomains.head
	
	
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
  * @since 10.07.2025, v0.4
  */
case class AccessRequestPath[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessRequestPath[A]] with FilterRequestPaths[AccessRequestPath[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible request path
	  */
	lazy val values = AccessRequestPathValue(wrapped)
	
	/**
	  * A copy of this access which also targets domain
	  */
	lazy val joinedToDomain = join(LogosTables.domain)
	
	/**
	  * Access to the values of linked domain
	  */
	lazy val domain = AccessDomainValue(joinedToDomain)
	
	/**
	  * Access to domain -based filtering functions
	  */
	lazy val whereDomain = FilterByDomain(joinedToDomain)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessRequestPath(newTarget)
}

