package utopia.nexus.model.servlet

import java.nio.charset.Charset
import scala.language.implicitConversions

object ParameterEncoding
{
	// ATTRIBUTES   --------------------
	
	/**
	 * A value indicating that no parameter encoding is applied
	 */
	lazy val none = apply(None)
	
	
	// IMPLICIT ------------------------
	
	/**
	 * Implicitly converts a character-set into parameter encoding
	 * @param charset Charset used in the encoding
	 * @return Parameter encoding wrapping the specified character-set
	 */
	implicit def apply(charset: Charset): ParameterEncoding = apply(Some(charset))
}

/**
 * Used for specifying expected parameter encoding implicitly
 * @author Mikko Hilpinen
 * @since 09.11.2025, v2.0
 */
case class ParameterEncoding(charset: Option[Charset])
