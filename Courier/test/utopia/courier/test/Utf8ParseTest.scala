package utopia.courier.test

import utopia.courier.model.EmailAddress
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.parse.string.StringFrom

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.mail.internet.MimeUtility
import scala.util.Try

/**
 * Tests UTF-8 parsing from sender name
 * @author Mikko Hilpinen
 * @since 15.10.2023, v1.1
 */
object Utf8ParseTest extends App
{
	//noinspection SameParameterValue
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
	
	println(s"UTF-8 processed \"=?UTF-8?B?QWlyIEV1cm9wYQ==?=\" is \"${processUtf8Encoding("=?UTF-8?B?QWlyIEV1cm9wYQ==?=")}\"")
	
	val address = EmailAddress("=?UTF-8?B?QWlyIEV1cm9wYQ==?= <info@aireuropanews.com>")
	println(address.addressPart)
	println(address.namePart)
}
