package utopia.courier.model

import utopia.flow.operator.EqualsExtensions._
import utopia.flow.parse.string.{Regex, StringFrom}
import utopia.flow.util.StringExtensions._

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.mail.internet.MimeUtility
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
				processUtf8Encoding(addressString.take(addressRange.start).trim)
					.notStartingWith("\"").notEndingWith("\"").trim)
		case None => apply(addressString, "")
	}
	
	// Handles UTF-8 encoded input
	private def processUtf8Encoding(input: String) = {
		val parts = input.split('?')
		// Case: UTF-8 Base64 encoding applied => Parses the string, if possible
		if (parts.length >= 4 && (parts(1) ~== "UTF-8")) {
			parts(2).toUpperCase match {
				// Case: Base64 encoding used
				case "B" =>
					Try {
						val decodedBytes = Base64.getDecoder.decode(parts(3))
						new String(decodedBytes, "UTF-8")
					}.getOrElse { input }
				// Case: "Quoted-printable" -encoding used
				case "Q" =>
					Try {
						StringFrom.stream(MimeUtility.decode(
							new ByteArrayInputStream(parts(3).getBytes(StandardCharsets.UTF_8)),
							"quoted-printable"))
					}.flatten.getOrElse(input)
				case _ => input
			}
		}
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
