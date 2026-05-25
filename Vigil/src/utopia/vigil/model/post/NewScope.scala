package utopia.vigil.model.post

import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties

import scala.util.{Failure, Success, Try}

object NewScope extends FromModelFactory[NewScope]
{
	// ATTRIBUTES   ---------------------
	
	private val schema = ModelDeclaration(PropertyDeclaration("name", StringType, Single("key")))
	
	
	// IMPLEMENTED  ---------------------
	
	// Makes sure "name" is specified
	override def apply(model: HasProperties): Try[NewScope] = schema.validate(model).flatMap { model =>
		model
			// Parses "grants", if present
			.tryGet("grants") {
				_.tryVectorWith { value =>
					// Interprets each value into a new scope
					value.getModelOrString match {
						// Case: Value specified as a model => Parses it recursively
						case Left(model) => apply(model)
						// Case: Value specified as a string => Fails if empty
						case Right(name) =>
							if (name.isEmpty)
								Failure(new IllegalArgumentException(s"Can't interpret `$value` as a scope"))
							else
								Success(apply(name))
					}
				}
			}
			.map { grants => apply(model("name").getString, grants) }
	}
}

/**
 * A POST model used for creating new scopes
 * @param name Name / key of this scope
 * @param grants Granted scopes
 * @author Mikko Hilpinen
 * @since 24.05.2026, v0.1
 */
case class NewScope(name: String, grants: Seq[NewScope] = Empty)
{
	/**
	 * @return Scope-links involved in this scope.
	 *         Each value contains the name of the parent scope, with the name of the granted child scope.
	 */
	def links: Seq[Pair[String]] = {
		if (grants.isEmpty)
			Empty
		else
			grants.flatMap { granted => Pair(name, granted.name) +: granted.links }
	}
}