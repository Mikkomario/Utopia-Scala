package utopia.echo.model.request.tts.piper

import scala.language.implicitConversions

object Voice
{
	// IMPLICIT ------------------------
	
	/**
	 * @param name Name of this voice, as recognized by the Piper API.
	 *             E.g. "en_US-lessac-medium".
	 * @return A voice instance wrapping the specified name
	 */
	implicit def apply(name: String): Voice = _Voice(name)
	
	
	// NESTED   ------------------------
	
	private case class _Voice(name: String) extends Voice
}

/**
 * Used for specifying the voice model in text-to-speech
 * @author Mikko Hilpinen
 * @since 28.09.2025, v1.4
 */
trait Voice
{
	// ABSTRACT ----------------------
	
	/**
	 * @return Name of this voice, as recognized by the Piper API.
	 *         E.g. "en_US-lessac-medium".
	 */
	def name: String
	
	
	// IMPLEMENTED  ------------------
	
	override def toString: String = name
}
