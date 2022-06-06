package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual email validation purposes, based on their id
  * @author Mikko Hilpinen
  * @since 25.10.2021, v4.0
  */
case class DbSingleEmailValidationPurpose(id: Int) 
	extends UniqueEmailValidationPurposeAccess with SingleIntIdModelAccess[EmailValidationPurpose]

