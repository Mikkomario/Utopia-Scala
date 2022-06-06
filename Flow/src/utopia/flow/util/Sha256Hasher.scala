package utopia.flow.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import scala.util.Random

/**
 * Used for hashing string using the SHA-256 algorithm, plus possible salting
 * @author Mikko Hilpinen
 * @since 14.2.2022, v1.15
 */
// See: https://www.geeksforgeeks.org/sha-256-hash-in-java/
object Sha256Hasher
{
	// ATTRIBUTES   ------------------------
	
	private val saltLength = 8
	
	
	// OTHER    ----------------------------
	
	/**
	 * Hashes the specified input string
	 * @param string A string to hash
	 * @return Hashed string
	 */
	def apply(string: String) =
	{
		// Acquires the hash byte array
		val hashBytes = MessageDigest.getInstance("SHA-256").digest(string.getBytes(StandardCharsets.UTF_8))
		// Converts the bytes to a hex string
		BigInt(hashBytes).toString(16)
	}
	
	/**
	 * Hashes the specified input string
	 * @param string A string to hash
	 * @param salt A salt to use (default = random)
	 * @return Hashed string, prepended with the salt used, separated with ':'
	 */
	def salted(string: String, salt: String = randomSalt()) =
	{
		val hex = apply(salt + string)
		// Adds the salt to the beginning, if one was specified
		if (salt.nonEmpty) s"$salt:$hex" else hex
	}
	
	/**
	 * Checks whether the specified input string matches that used to generate the specified hash
	 * @param string A string to test
	 * @param hash A previously generated hash, possibly including salt
	 * @return Whether the specified string matched that used to generate the specified hash
	 */
	def validate(string: String, hash: String) =
	{
		// Looks for the salt from the hash first
		val salt = Some(hash.indexOf(":")).filter { _ >= 0 } match {
			case Some(endIndex) => hash.take(endIndex)
			case None => ""
		}
		// Hashes the input with the same salt and compares the two hashes
		salted(string, salt) == hash
	}
	
	private def randomSalt() = Random.alphanumeric.take(saltLength).mkString
}
