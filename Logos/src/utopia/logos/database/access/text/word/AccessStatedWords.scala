package utopia.logos.database.access.text.word

import placement.{AccessWordPlacementValues, FilterWordPlacements}
import utopia.logos.database.factory.text.StatedWordDbFactory
import utopia.logos.model.combined.text.StatedWord
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vault.sql.Condition

import scala.language.implicitConversions

object AccessStatedWords extends AccessManyRoot[AccessStatedWords[StatedWord]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(AccessManyRows(StatedWordDbFactory))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessStatedWords[_]): AccessWordValues = access.values
}

/**
  * Used for accessing multiple stated words from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessStatedWords[A](wrapped: TargetingManyRows[A]) 
	extends AccessRowsWrapper[A, AccessStatedWords[A], AccessStatedWord[A]] 
		with FilterWords[AccessStatedWords[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible stated words
	  */
	lazy val values = AccessWordValues(wrapped)
	/**
	  * Access to word placement -specific values
	  */
	lazy val useCases = AccessWordPlacementValues(wrapped)
	
	
	// COMPUTED	--------------------
	
	/**
	  * Access to use case -based filtering functions
	  */
	def whereUseCases = FilterByUseCase
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessStatedWords(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessStatedWord(target)
	
	
	// NESTED	--------------------
	
	/**
	  * An interface for use case -based filtering
	  * @since 01.06.2025
	  */
	object FilterByUseCase extends FilterWordPlacements[AccessStatedWords[A]]
	{
		// IMPLEMENTED	--------------------
		
		override protected def self: AccessStatedWords[A] = AccessStatedWords.this
		override def accessCondition = AccessStatedWords.this.accessCondition
		override def table = AccessStatedWords.this.table
		override def target = AccessStatedWords.this.target
		
		override def apply(condition: Condition) = AccessStatedWords.this(condition)
	}
}

