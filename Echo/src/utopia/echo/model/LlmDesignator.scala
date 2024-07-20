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
}

/**
  * An interface for choosing a LLM using its name
  * @author Mikko Hilpinen
  * @since 11.07.2024, v1.0
  */
case class LlmDesignator(name: String)
