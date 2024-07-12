package utopia.echo.model

import scala.language.implicitConversions

object LlmDesignator
{
	implicit def designateWithName(name: String): LlmDesignator = apply(name)
}

/**
  * An interface for choosing a LLM using its name
  * @author Mikko Hilpinen
  * @since 11.07.2024, v0.1
  */
case class LlmDesignator(name: String)
