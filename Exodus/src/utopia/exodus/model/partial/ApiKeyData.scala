package utopia.exodus.model.partial

import java.time.Instant

/**
  * Used for storing api keys to DB or accessing their data
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  * @param key A unique key used by the client to authorize themselves
  * @param name A descriptive name of the ownership or role of this api key
  * @param creationTime The time when this key was created / generated / first allowed
  */
case class ApiKeyData(key: String, name: String, creationTime: Instant = Instant.now())
