package utopia.courier.model

import utopia.flow.operator.EqualsExtensions._
import utopia.flow.parse.string.Regex

import java.util.Base64
import scala.language.implicitConversions
import scala.util.Try

object EmailAddress
{
	// ATTRIBUTES   -----------------------
	
	private val addressPartRegex =
		Regex.escape('<') + Regex.any + Regex("@") + Regex.any + Regex.escape('>')
	
	
	// OTHER    ---------------------------
	
	/**
	 * Parses an email address from a string
	 * @param addressString A string that represents an email address
	 * @return Parsed email address
	 */
	implicit def apply(addressString: String): EmailAddress = addressPartRegex.firstRangeFrom(addressString) match {
		case Some(addressRange) =>
			val addressPart = addressString.slice(addressRange.start + 1, addressRange.last)
			apply(
				addressPart,
				processBase64Utf8Encoding(addressString.take(addressRange.start).trim))
		case None => apply(addressString, "")
	}
	
	// Handles UTF-8 encoded input
	private def processBase64Utf8Encoding(input: String) = {
		val parts = input.split('?')
		// Case: UTF-8 Base64 encoding applied => Parses the string, if possible
		if (parts.length >= 4 && (parts(1) ~== "UTF-8") && (parts(2) ~== "B")) {
			Try {
				val decodedBytes = Base64.getDecoder.decode(parts(3))
				new String(decodedBytes, "UTF-8")
			}.getOrElse { input }
		}
		// Case: Other type of input => Preserves the input as is
		else
			input
	}
}

/**
 * Represents an email address. May be associated with a name.
 * @author Mikko Hilpinen
 * @since 13.10.2023, v1.1
 * @param addressPart Part that represents an email address
 * @param namePart Part that represents the name of the associated person. May be empty.
 */
case class EmailAddress(addressPart: String, namePart: String)
{
	override def toString = if (namePart.isEmpty) addressPart else s"$namePart <$addressPart>"
}
