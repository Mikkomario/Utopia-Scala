package utopia.logos.database.access.text.delimiter

import utopia.logos.model.stored.text.Delimiter
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessDelimiter extends AccessOneRoot[AccessDelimiter[Delimiter]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessDelimiters.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessDelimiter[_]): AccessDelimiterValue = access.values
}

/**
  * Used for accessing individual delimiters from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessDelimiter[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessDelimiter[A]] with FilterDelimiters[AccessDelimiter[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible delimiter
	  */
	lazy val values = AccessDelimiterValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessDelimiter(newTarget)
}

