package utopia.citadel.database.model.user

import java.time.Instant
import utopia.citadel.database.factory.user.UserFactory
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.user.UserData
import utopia.metropolis.model.stored.user.User
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing UserModel instances and for inserting Users to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserModel extends DataInserter[UserModel, User, UserData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains User created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains User created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = UserFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: UserData) = apply(None, Some(data.created))
	
	override def complete(id: Value, data: UserData) = User(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * Inserts a new user to the DB
	  * @param connection Implicit DB Connection
	  * @return Inserted user
	  */
	def insert()(implicit connection: Connection): User = insert(UserData())
	
	/**
	  * @param created Time when this User was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	/**
	  * @param id A User id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
}

/**
  * Used for interacting with Users in the database
  * @param id User database id
  * @param created Time when this User was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserModel(id: Option[Int] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[User]
{
	// IMPLEMENTED	--------------------
	
	override def factory = UserModel.factory
	
	override def valueProperties = 
	{
		import UserModel._
		Vector("id" -> id, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
}

