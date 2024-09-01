package utopia.echo.model.enumeration

import utopia.flow.collection.CollectionExtensions._
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
	
	
	// VALUES   -----------------------
	
	/**
	  * Role belonging to the client prompting the LLM
	  */
	case object User extends ChatRole
	{
		override val name: String = "user"
		override def opposite = Assistant
	}
	
	/**
	  * Role belonging to the general system between the user and the LLM
	  */
	case object System extends ChatRole
	{
		override val name: String = "system"
		override def opposite: ChatRole = this
	}
	
	/**
	  * Role belonging to the LLM
	  */
	case object Assistant extends ChatRole
	{
		override val name: String = "assistant"
		override def opposite = User
	}
	
	/**
	  * Role representing additional tools provided for the LLM
	  */
	case object Tool extends ChatRole
	{
		override def name: String = "tool"
		override def opposite: ChatRole = Assistant
	}
}