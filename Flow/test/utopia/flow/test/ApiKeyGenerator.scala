package utopia.flow.test

import utopia.flow.parse.Sha256Hasher

import java.util.UUID

/**
  * Generates and hashes randomly generated API-keys
  * @author Mikko Hilpinen
  * @since 18.8.2022, v1.16
  */
object ApiKeyGenerator extends App
{
	val apiKey = UUID.randomUUID().toString
	val hash = Sha256Hasher(apiKey)
	
	println(s"Key: $apiKey")
	println(s"Hash: $hash")
	println(s"Hash Length: ${hash.length}")
}
