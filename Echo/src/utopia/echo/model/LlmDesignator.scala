package utopia.echo.model

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
trait LlmDesignator
{
	// ABSTRACT -----------------------
	
	/**
	 * @return Name of the designated / targeted LLM
	 */
	def llmName: String
	
	
	// IMPLEMENTED  -------------------
	
	override def toString = llmName
}
