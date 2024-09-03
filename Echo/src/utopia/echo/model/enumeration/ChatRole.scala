package utopia.echo.model.enumeration

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType.{IntType, StringType}
import utopia.flow.operator.equality.EqualsExtensions._

/**
  * An enumeration for different message sender roles understood by the Ollama interface.
  * Namely: user, assistant & system
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
sealed trait ChatRole
{
	// ABSTRACT -----------------------
	
	/**
	 * @return An integer which represents this role
	 */
	def id: Int
	/**
	  * @return Name of this role
	  */
	def name: String
	/**
	  * @return The role opposite to this one, if applicable.
	  *         If this role is System, returns self.
	  */
	def opposite: ChatRole
}

object ChatRole
{
	// ATTRIBUTES   -------------------
	
	/**
	  * All chat roles
	  */
	val values = Vector[ChatRole](User, System, Assistant, Tool)
	
	
	// OTHER    -----------------------
	
	/**
	  * @param roleName Name of the targeted role
	  * @return Role matching that name. None if no such role was found.
	  */
	def findForName(roleName: String) = values.find { _.name ~== roleName }
	/**
	  * @param roleName Name of the targeted role
	  * @return Role matching that name. Failure if no such role was found.
	  */
	def forName(roleName: String) =
		findForName(roleName).toTry { new NoSuchElementException(s"No chat role matches '$roleName'") }
	
	/**
	 * @param id A chat role id
	 * @return A chat role with that id. None if none of the roles had that id.
	 */
	def findForId(id: Int) = values.find { _.id == id }
	/**
	 * @param id Targeted id
	 * @return Chat role with that id, or a failure if no role had that id
	 */
	def forId(id: Int) =
		findForId(id).toTry { new NoSuchElementException(s"None of the chat roles match id $id") }
	
	/**
	 * @param roleValue A value which represents a chat role (either an id or a role name)
	 * @return Chat role represented by the specified value. Failure if the value didn't represent any chat role.
	 */
	def fromValue(roleValue: Value) = roleValue.castTo(IntType, StringType) match {
		case Left(idVal) =>
			idVal.int
				.toTry { new IllegalArgumentException(
					s"${ roleValue.description } could not be cast to either role id or role name") }
				.flatMap(forId)
			
		case Right(strVal) => forName(strVal.getString)
	}
	
	
	// VALUES   -----------------------
	
	/**
	  * Role belonging to the client prompting the LLM
	  */
	case object User extends ChatRole
	{
		override def id: Int = 1
		override val name: String = "user"
		override def opposite = Assistant
	}
	
	/**
	  * Role belonging to the general system between the user and the LLM
	  */
	case object System extends ChatRole
	{
		override def id: Int = 2
		override val name: String = "system"
		override def opposite: ChatRole = this
	}
	
	/**
	  * Role belonging to the LLM
	  */
	case object Assistant extends ChatRole
	{
		override def id: Int = 3
		override val name: String = "assistant"
		override def opposite = User
	}
	
	/**
	  * Role representing additional tools provided for the LLM
	  */
	case object Tool extends ChatRole
	{
		override def id: Int = 4
		override def name: String = "tool"
		override def opposite: ChatRole = Assistant
	}
}