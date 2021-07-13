package utopia.ambassador.model.stored.service

import java.time.Instant

/**
  * Represents a 3rd party service towards which OAuth process is targeted
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
case class AuthService(id: Int, name: String, created: Instant)
