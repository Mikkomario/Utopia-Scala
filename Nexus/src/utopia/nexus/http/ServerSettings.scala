package utopia.nexus.http

import scala.io.Codec

/**
 * Server settings specify values commonly used by server-side resources
 * @author Mikko Hilpinen
 * @since 17.9.2017
 * @param address The server address, including domain name and possible port. Should not end with a 
 * directory separator ('/')
 * @param expectedParameterEncoding Encoding that is expected for query parameters.
 *                                  None if no decoding should be done (default)
 */
case class ServerSettings(address: String, expectedParameterEncoding: Option[Codec] = None)