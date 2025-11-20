package utopia.logos.database.access.text.word

import placement.{AccessWordPlacementValue, FilterWordPlacements}
import utopia.logos.model.combined.text.StatedWord
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import utopia.vault.sql.Condition

import scala.language.implicitConversions

object AccessStatedWord extends AccessOneRoot[AccessStatedWord[StatedWord]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessStatedWords.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessStatedWord[_]): AccessWordValue = access.values
}

/**
  * Used for accessing individual stated words from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessStatedWord[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessStatedWord[A]] with FilterWords[AccessStatedWord[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible stated word
	  */
	lazy val values = AccessWordValue(wrapped)
	/**
	  * Access to word placement -specific values
	  */
	lazy val useCase = AccessWordPlacementValue(wrapped)
	
	
	// COMPUTED	--------------------
	
	/**
	  * Access to use case -based filtering functions
	  */
	def whereUseCase = FilterByUseCase
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessStatedWord(newTarget)
	
	
	// NESTED	--------------------
	
	/**
	  * An interface for use case -based filtering
	  * @since 01.06.2025
	  */
	object FilterByUseCase extends FilterWordPlacements[AccessStatedWord[A]]
	{
		// IMPLEMENTED	--------------------
		
		override def self: AccessStatedWord[A] = AccessStatedWord.this
		override def accessCondition = AccessStatedWord.this.accessCondition
		override def table = AccessStatedWord.this.table
		override def target = AccessStatedWord.this.target
		
		override def apply(condition: Condition) = AccessStatedWord.this(condition)
	}
}

