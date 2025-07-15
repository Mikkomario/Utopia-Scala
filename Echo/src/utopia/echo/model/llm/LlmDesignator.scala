package utopia.echo.model.llm

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
	def apply(llmName: String, thinks: Boolean = false): LlmDesignator = _LlmDesignator(llmName, thinks)
	
	
	// NESTED   ---------------------
	
	private case class _LlmDesignator(llmName: String, thinks: Boolean) extends LlmDesignator
	{
		override def thinking: LlmDesignator = if (thinks) this else copy(thinks = true)
	}
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
	/**
	  * @return Whether the designated LLM thinks / reflects by default
	  */
	def thinks: Boolean
	
	/**
	  * @return A copy of this LLM designator which indicates that the LLM thinks / reflects
	  */
	def thinking: LlmDesignator
	
	
	// IMPLEMENTED  -------------------
	
	override def self: LlmDesignator = this
	
	override def toString = llmName
}
