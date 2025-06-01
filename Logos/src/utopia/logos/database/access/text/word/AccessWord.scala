package utopia.logos.database.access.text.word

import utopia.logos.model.stored.text.StoredWord
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessWord extends AccessOneRoot[AccessWord[StoredWord]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessWords.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessWord[_]): AccessWordValue = access.values
}

/**
  * Used for accessing individual words from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessWord[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessWord[A]] with FilterWords[AccessWord[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible word
	  */
	lazy val values = AccessWordValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessWord(newTarget)
}

