package utopia.echo.model

import utopia.flow.operator.ScopeUsable

import scala.language.implicitConversions

object LlmDesignator
{
	// ATTRIBUTES   -----------------
	
	/**
	  * An empty LLM designator
	  */
	val empty = apply("")
	
	
	// IMPLICIT ---------------------
	
	implicit def designateWithName(name: String): LlmDesignator = apply(name)
	
	
	// OTHER    ---------------------
	
	/**
	 * @param llmName Name of the targeted LLM
	 * @return An LLM designator targeting that LLM
	 */
	def apply(llmName: String): LlmDesignator = _LlmDesignator(llmName)
	
	
	// NESTED   ---------------------
	
	private case class _LlmDesignator(llmName: String) extends LlmDesignator
}

/**
  * An interface for choosing a LLM using its name
  * @author Mikko Hilpinen
  * @since 11.07.2024, v1.0
  */
trait LlmDesignator extends ScopeUsable[LlmDesignator]
{
	// ABSTRACT -----------------------
	
	/**
	 * @return Name of the designated / targeted LLM
	 */
	def llmName: String
	
	
	// IMPLEMENTED  -------------------
	
	override def self: LlmDesignator = this
	
	override def toString = llmName
}
