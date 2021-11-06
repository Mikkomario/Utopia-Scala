package utopia.vault.coder.controller.writer.model

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.writer.database.AccessWriter
import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, File}
import utopia.vault.coder.model.scala.{DeclarationDate, Parameter, Reference, declaration}
import utopia.vault.coder.util.NamingUtils

import scala.io.Codec

/**
  * Used for writing model data from class data
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
object ModelWriter
{
	/**
	  * Writes stored and partial model classes for a class template
	  * @param classToWrite class being written
	  * @param codec        Implicit codec used when writing files (implicit)
	  * @param setup        Target project -specific settings (implicit)
	  * @return Reference to the stored version, followed by a reference to the data version. Failure if writing failed.
	  */
	def apply(classToWrite: Class)(implicit codec: Codec, setup: ProjectSetup) =
	{
		val dataClassName = classToWrite.name.singular + "Data"
		val dataClassPackage = setup.modelPackage / s"partial.${ classToWrite.packageName }"
		val propWrites = classToWrite.properties.map { prop =>
			val propNameInModel = NamingUtils.camelToUnderscore(prop.name.singular).quoted
			prop.toValueCode.withPrefix(propNameInModel + " -> ")
		}
		val propWriteCode = if (propWrites.isEmpty) CodePiece("Model.empty", Set(Reference.model)) else
			propWrites.reduceLeft { _.append(_, ", ") }.withinParenthesis.withPrefix("Vector")
				.withinParenthesis.withPrefix("Model").referringTo(Reference.model)
		
		// Writes the data model
		File(dataClassPackage,
			ClassDeclaration(dataClassName,
				// Accepts a copy of each property. Uses default values where possible.
				classToWrite.properties.map { prop =>
					Parameter(prop.name.singular, prop.dataType.toScala, prop.default,
						description = prop.description)
				},
				// Extends ModelConvertible
				Vector(Reference.modelConvertible),
				// Implements the toModel -property
				properties = deprecationPropertiesFor(classToWrite) :+
					ComputedProperty("toModel", propWriteCode.references, isOverridden = true)(propWriteCode.text),
				description = classToWrite.description, author = classToWrite.author,
				since = DeclarationDate.versionedToday, isCaseClass = true)
		).write().flatMap { dataClassRef =>
			val storePackage = setup.modelPackage / s"stored.${ classToWrite.packageName }"
			// Writes the stored model next
			val storedClass = {
				val idType = classToWrite.idType.toScala
				// Accepts id and data -parameters
				val constructionParams = Vector(
					Parameter("id", idType, description = s"id of this ${ classToWrite.name } in the database"),
					Parameter("data", dataClassRef, description = s"Wrapped ${ classToWrite.name } data")
				)
				// May provide a utility access method
				val accessProperty = {
					if (setup.modelCanReferToDB)
					{
						val singleAccessRef = AccessWriter.singleIdReferenceFor(classToWrite)
						Some(ComputedProperty("access", Set(singleAccessRef),
							description = s"An access point to this ${ classToWrite.name } in the database")(
							s"${ singleAccessRef.target }(id)"))
					}
					else
						None
				}
				
				val description = s"Represents a ${ classToWrite.name } that has already been stored in the database"
				// ModelConvertible extension & implementation differs based on id type
				// Also, the Stored extension differs based on whether Vault-dependency is allowed
				if (classToWrite.useLongId)
					ClassDeclaration(classToWrite.name.singular, constructionParams,
						Vector(Reference.stored(dataClassRef, idType)),
						properties = Vector(
							ComputedProperty("toModel", Set(Reference.valueConversions, Reference.constant),
								isOverridden = true)("Constant(\"id\", id) + data.toModel"),
						) ++ accessProperty, description = description, isCaseClass = true)
				else
				{
					val parent = if (setup.modelCanReferToDB) Reference.storedModelConvertible(dataClassRef) else
						Reference.metropolisStoredModelConvertible(dataClassRef)
					declaration.ClassDeclaration(classToWrite.name.singular, constructionParams, Vector(parent),
						properties = accessProperty.toVector,
						description = description, author = classToWrite.author, since = DeclarationDate.versionedToday,
						isCaseClass = true)
				}
			}
			File(storePackage, storedClass).write().map { _ -> dataClassRef }
		}
	}
	
	// Deprecation-supporting classes can have custom properties
	private def deprecationPropertiesFor(classToWrite: Class) =
	{
		classToWrite.deprecationProperty match {
			case Some(prop) =>
				Vector(
					ComputedProperty("isDeprecated",
						description = s"Whether this ${ classToWrite.name } has already been deprecated")(
						s"${ prop.name }.isDefined"),
					ComputedProperty("isValid",
						description = s"Whether this ${ classToWrite.name } is still valid (not deprecated)")(
						"!isDeprecated")
				)
			case None =>
				classToWrite.expirationProperty match {
					case Some(prop) =>
						Vector(
							ComputedProperty("hasExpired", Set(Reference.timeExtensions, Reference.now),
								description = s"Whether this ${
									classToWrite.name
								} is no longer valid because it has expired")(
								s"${ prop.name } <= Now"),
							ComputedProperty("isValid",
								description = s"Whether this ${ classToWrite.name } is still valid (hasn't expired yet)")(
								"!hasExpired")
						)
					case None => Vector()
				}
		}
	}
}
