package utopia.metropolis.model.post

import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible, StringType, VectorType}
import utopia.flow.util.CollectionExtensions._

object NewUser extends FromModelFactory[NewUser]
{
	private val schema = ModelDeclaration("name" -> StringType, "password" -> StringType, "languages" -> VectorType)
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		// Languages must be parseable
		valid("languages").getVector.tryMap { v => NewLanguageProficiency(v.getModel) }.map { languages =>
			NewUser(valid("name").getString, valid("password").getString, languages,
				valid("email").string, valid("request_refresh_token", "remember_me").getBoolean)
		}
	}
}

/**
  * A model used when creating new users from client side
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param name User name
  * @param password Initial user password
  * @param languages List of the languages known by the user (with levels of familiarity)
  * @param email Email address to assign for this user
  * @param requestRefreshToken Whether a refresh token should be generated and returned upon user creation
  *                            (default = false)
  */
case class NewUser(name: String, password: String, languages: Vector[NewLanguageProficiency],
                   email: Option[String] = None, requestRefreshToken: Boolean = false)
	extends ModelConvertible
{
	// COMPUTED	-----------------------------
	
	/**
	  * @return Whether email has been specified in this user data
	  */
	def specifiesEmail = email.isDefined
	
	
	// IMPLEMENTED	-------------------------
	
	override def toModel = {
		Model(Vector[(String, Value)]("name" -> name, "email" -> email, "password" -> password,
			"languages" -> languages.map { _.toModel }, "request_refresh_token" -> requestRefreshToken))
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param email An email address
	  * @return A copy of this model with that email address
	  */
	def withEmailAddress(email: String) = copy(email = Some(email))
}
