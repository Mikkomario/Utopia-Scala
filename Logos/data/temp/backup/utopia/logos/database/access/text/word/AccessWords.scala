package utopia.logos.database.access.text.word

import utopia.logos.database.factory.text.WordDbFactory
import utopia.logos.model.stored.text.StoredWord
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessWords extends AccessManyRoot[AccessWords[StoredWord]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(AccessManyRows(WordDbFactory))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessWords[_]): AccessWordValues = access.values
}

/**
  * Used for accessing multiple words from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessWords[A](wrapped: TargetingManyRows[A]) 
	extends AccessRowsWrapper[A, AccessWords[A], AccessWord[A]] with FilterWords[AccessWords[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible words
	  */
	lazy val values = AccessWordValues(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessWords(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessWord(target)
}

