package utopia.logos.database.access.url.link

import utopia.logos.model.stored.url.StoredLink
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessLink extends AccessOneRoot[AccessLink[StoredLink]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessLinks.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessLink[_]): AccessLinkValue = access.values
}

/**
  * Used for accessing individual links from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessLink[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessLink[A]] with FilterLinks[AccessLink[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible link
	  */
	lazy val values = AccessLinkValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessLink(newTarget)
}

