package utopia.exodus.database.factory.user

import utopia.exodus.database.ExodusTables
import utopia.exodus.model.partial.user.UserPasswordData
import utopia.exodus.model.stored.user.UserPassword
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading UserPassword data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object UserPasswordFactory extends FromValidatedRowModelFactory[UserPassword]
{
	// IMPLEMENTED	--------------------
	
	override def table = ExodusTables.userPassword
	
	override def fromValidatedModel(valid: Model[Constant]) = 
		UserPassword(valid("id").getInt, UserPasswordData(valid("userId").getInt, valid("hash").getString, 
			valid("created").getInstant))
}

