package utopia.genesis.handling.event.consume

import scala.annotation.unused
import scala.language.implicitConversions

/**
  * Represents a (listener's) choice to either consume an item, or to keep it unconsumed
  * @author Mikko Hilpinen
  * @since 05/02/2024, v4.0
  */
sealed trait ConsumeChoice
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The consume event to generate based on this choice.
	  *         None if the choice was not to consume the related item.
	  */
	def eventIfConsumed: Option[ConsumeEvent]
	
	/**
	  * @param other Another consume choice
	  * @return A choice that chooses to consume if either of these choices is to consume
	  */
	def ||(other: ConsumeChoice): ConsumeChoice
}

object ConsumeChoice
{
	// IMPLICIT -----------------------
	
	implicit def preserveByDefault(@unused u: Unit): ConsumeChoice = Preserve
	
	
	// OTHER    -----------------------
	
	/**
	  * @param event A consume event. None if chose not to consume.
	  * @return A consume choice that yields the specified event.
	  */
	def apply(event: Option[ConsumeEvent]): ConsumeChoice = event match {
		case Some(event) => Consume(event)
		case None => Preserve
	}
	
	
	// NESTED   -----------------------
	
	/**
	  * Represents a choice not to consume an item
	  */
	case object Preserve extends ConsumeChoice
	{
		override def eventIfConsumed: Option[ConsumeEvent] = None
		
		override def ||(other: ConsumeChoice): ConsumeChoice = other
	}
	
	object Consume
	{
		/**
		  * @param by A string that represents this consumer / consume event (call-by-name)
		  * @return A choice to consume the related item
		  */
		def apply(by: => String): Consume = new Consume(ConsumeEvent(by))
	}
	/**
	  * Represents a choice to consume an item
	  * @param consumeEvent Consume event generated by this choice
	  */
	case class Consume(consumeEvent: ConsumeEvent) extends ConsumeChoice
	{
		// IMPLEMENTED  --------------------
		
		override def eventIfConsumed: Option[ConsumeEvent] = Some(consumeEvent)
		
		override def ||(other: ConsumeChoice): ConsumeChoice = this
		
		
		// OTHER    ------------------------
		
		/**
		  * @param condition A condition for consuming
		  * @return Copy of this choice which is switched to Preserve if the specified condition is met
		  */
		def butOnlyIf(condition: Boolean): ConsumeChoice = if (condition) this else Preserve
	}
}
