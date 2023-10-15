package utopia.courier.test

import utopia.courier.model.EmailAddress
import utopia.flow.operator.EqualsExtensions._

import java.util.Base64
import scala.util.Try

/**
 * Tests UTF-8 parsing from sender name
 * @author Mikko Hilpinen
 * @since 15.10.2023, v1.1
 */
object Utf8ParseTest extends App
{
	//noinspection SameParameterValue
	private def processBase64Utf8Encoding(input: String) = {
		val parts = input.split('?')
		// Case: UTF-8 Base64 encoding applied => Parses the string, if possible
		if (parts.length == 5 && (parts(1) ~== "UTF-8") && (parts(2) ~== "B")) {
			Try {
				val decodedBytes = Base64.getDecoder.decode(parts(3))
				new String(decodedBytes, "UTF-8")
			}.getOrElse { input }
		}
		// Case: Other type of input => Preserves the input as is
		else
			input
	}
	
	println(s"UTF-8 processed \"=?UTF-8?B?QWlyIEV1cm9wYQ==?=\" is \"${processBase64Utf8Encoding("=?UTF-8?B?QWlyIEV1cm9wYQ==?=")}\"")
	
	val address = EmailAddress("=?UTF-8?B?QWlyIEV1cm9wYQ==?= <info@aireuropanews.com>")
	println(address.addressPart)
	println(address.namePart)
}
