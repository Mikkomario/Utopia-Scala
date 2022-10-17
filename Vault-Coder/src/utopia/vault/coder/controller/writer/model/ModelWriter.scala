package utopia.vault.coder.controller.writer.model

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.writer.database.AccessWriter
import utopia.vault.coder.model.data.{Class, DbProperty, Name, NamingRules, ProjectSetup, Property}
import utopia.vault.coder.model.enumeration.NamingConvention.{CamelCase, UnderScore}
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.datatype.{Reference, ScalaType}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, LazyValue}
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, File, ObjectDeclaration}
import utopia.vault.coder.model.scala.{DeclarationDate, Parameter, declaration}
import utopia.vault.coder.util.ClassMethodFactory

import scala.io.Codec

/**
  * Used for writing model data from class data
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
object ModelWriter
{
	private val dataClassAppendix = Name("Data", "Data", CamelCase.capitalized)
	
	/**
	  * Writes stored and partial model classes for a class template
	  * @param classToWrite class being written
	  * @param codec        Implicit codec used when writing files (implicit)
	  * @param setup        Target project -specific settings (implicit)
	  * @return Reference to the stored version, followed by a reference to the data version. Failure if writing failed.
	  */
	def apply(classToWrite: Class)
	         (implicit codec: Codec, setup: ProjectSetup, naming: NamingRules) =
	{
		val dataClassName = (classToWrite.name + dataClassAppendix).className
		val dataClassPackage = setup.modelPackage / s"partial.${ classToWrite.packageName }"
		val propWrites = classToWrite.properties.map { prop =>
			val propNameInModel = prop.jsonPropName.quoted
			prop.toValueCode.withPrefix(propNameInModel + " -> ")
		}
		val propWriteCode = if (propWrites.isEmpty) CodePiece("Model.empty", Set(Reference.model)) else
			propWrites.reduceLeft { _.append(_, ", ") }.withinParenthesis.withPrefix("Vector")
				.withinParenthesis.withPrefix("Model").referringTo(Reference.model)
		val fromModelMayFail = classToWrite.fromDbModelConversionMayFail
		val modelDeclarationCode = Reference.modelDeclaration.targetCode +
			(CodePiece("Vector") +
				classToWrite.properties.map(propertyDeclarationFrom).reduceLeftOption { _.append(_, ", ") }
					.getOrElse(CodePiece.empty).withinParenthesis
				).withinParenthesis
		// Writes the data model and the data object
		File(dataClassPackage,
			ObjectDeclaration(dataClassName,
				Vector(Reference.fromModelFactoryWithSchema(ScalaType.basic(dataClassName))),
				properties = Vector(
					LazyValue("schema", modelDeclarationCode.references, isOverridden = !fromModelMayFail,
						isLowMergePriority = true)(modelDeclarationCode.text)
				),
				methods = Set(fromModelFor(classToWrite, dataClassName).copy(isLowMergePriority = true))
			),
			ClassDeclaration(dataClassName,
				// Accepts a copy of each property. Uses default values where possible.
				constructionParams = classToWrite.properties.map { prop =>
					Parameter(prop.name.prop, prop.dataType.toScala, prop.defaultValue,
						description = prop.description)
				},
				// Extends ModelConvertible
				extensions = Vector(Reference.modelConvertible),
				// Implements the toModel -property
				properties = deprecationPropertiesFor(classToWrite) :+
					ComputedProperty("toModel", propWriteCode.references, isOverridden = true)(propWriteCode.text),
				description = classToWrite.description, author = classToWrite.author,
				since = DeclarationDate.versionedToday, isCaseClass = true)
		).write().flatMap { dataClassRef =>
			val storePackage = setup.modelPackage / s"stored.${ classToWrite.packageName }"
			// Writes the stored model and object next
			val storedClass = {
				val idType = classToWrite.idType.toScala
				// Accepts id and data -parameters
				val constructionParams = Vector(
					Parameter("id", idType, description = s"id of this ${ classToWrite.name.doc } in the database"),
					Parameter("data", dataClassRef, description = s"Wrapped ${ classToWrite.name.doc } data")
				)
				// May provide a utility access method
				val accessProperty = {
					if (setup.modelCanReferToDB) {
						val singleAccessRef = AccessWriter.singleIdReferenceFor(classToWrite)
						Some(ComputedProperty("access", Set(singleAccessRef),
							description = s"An access point to this ${ classToWrite.name.doc } in the database")(
							s"${ singleAccessRef.target }(id)"))
					}
					else
						None
				}
				
				val description = s"Represents a ${ classToWrite.name.doc } that has already been stored in the database"
				// ModelConvertible extension & implementation differs based on id type
				// Also, the Stored extension differs based on whether Vault-dependency is allowed
				if (classToWrite.useLongId)
					ClassDeclaration(classToWrite.name.className, constructionParams = constructionParams,
						extensions = Vector(Reference.stored(dataClassRef, idType)),
						properties = Vector(
							ComputedProperty("toModel", Set(Reference.valueConversions, Reference.constant),
								isOverridden = true)("Constant(\"id\", id) + data.toModel"),
						) ++ accessProperty, description = description, isCaseClass = true)
				else
				{
					val parent = if (setup.modelCanReferToDB) Reference.storedModelConvertible(dataClassRef) else
						Reference.metropolisStoredModelConvertible(dataClassRef)
					declaration.ClassDeclaration(classToWrite.name.className, constructionParams = constructionParams,
						extensions = Vector(parent), properties = accessProperty.toVector,
						description = description, author = classToWrite.author, since = DeclarationDate.versionedToday,
						isCaseClass = true)
				}
			}
			// If Metropolis is enabled, writes the fromModelFactory as well
			val storedObject = {
				if (setup.modelCanReferToDB)
					None
				else
					Some(ObjectDeclaration(storedClass.name,
						Vector(Reference.storedFromModelFactory(
							ScalaType.basic(storedClass.name), ScalaType.basic(dataClassName))),
						properties = Vector(
							ComputedProperty("dataFactory", Set(dataClassRef), isOverridden = true)(dataClassName)
						)
					))
			}
			File(storePackage, storedObject.toVector :+ storedClass, Set[Reference]())
				.write().map { _ -> dataClassRef }
		}
	}
	
	// Deprecation-supporting classes can have custom properties
	private def deprecationPropertiesFor(classToWrite: Class)(implicit naming: NamingRules) =
	{
		classToWrite.deprecationProperty match {
			case Some(prop) =>
				Vector(
					ComputedProperty("isDeprecated",
						description = s"Whether this ${ classToWrite.name.doc } has already been deprecated")(
						s"${ prop.name.prop }.isDefined"),
					ComputedProperty("isValid",
						description = s"Whether this ${ classToWrite.name.doc } is still valid (not deprecated)")(
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
								s"${ prop.name.prop } <= Now"),
							ComputedProperty("isValid",
								description = s"Whether this ${ classToWrite.name.doc } is still valid (hasn't expired yet)")(
								"!hasExpired")
						)
					case None => Vector()
				}
		}
	}
	
	private def propertyDeclarationFrom(prop: Property)(implicit naming: NamingRules): CodePiece = {
		val name = prop.jsonPropName
		// Supports some alternative names
		val altNames = (Set(CamelCase.lower, UnderScore)
			.flatMap { style =>
				(prop.name +: prop.dbProperties.map { p: DbProperty => p.name }).toSet
					.map { name: Name => name.singularIn(style) }
			} - name)
			.toVector.sorted
		// May specify a default value
		val default = prop.customDefaultValue.notEmpty.orElse {
			val dt = prop.dataType
			if (dt.supportsDefaultJsonValues) dt.nonEmptyDefaultValue.notEmpty else None
		}.map { v => prop.dataType.toValueCode(v.text).referringTo(v.references) }
		
		// Writes only the necessary code parts (i.e. omits duplicate default parameters)
		var paramsCode = CodePiece(name.quoted).append(prop.dataType.valueDataType.targetCode, ", ")
		if (altNames.nonEmpty || default.isDefined)
			paramsCode = paramsCode
				.append(s"Vector(${ altNames.map { _.quoted }.mkString(", ") })", ", ")
		default.foreach { default => paramsCode = paramsCode.append(default, ", ") }
		if (prop.dataType.isOptional || !prop.dataType.supportsDefaultJsonValues)
			paramsCode = paramsCode.append("isOptional = true", ", ")
		
		Reference.propertyDeclaration.targetCode + paramsCode.withinParenthesis
	}
	
	// FIXME: Now reads "schema.validate(model).flatMap{ valid => " (toTry is missing)
	// FIXME: Also, parent type is wrong
	private def fromModelFor(classToWrite: Class, dataClassName: String)(implicit naming: NamingRules) = {
		def _modelFromAssignments(assignments: CodePiece) =
			assignments.withinParenthesis.withPrefix(dataClassName)
		
		if (classToWrite.fromDbModelConversionMayFail)
			ClassMethodFactory.classFromModel(classToWrite, "schema.validate(model)"){
				_.dbProperties.map { _.jsonPropName } }(_modelFromAssignments)
		else
			ClassMethodFactory.classFromValidatedModel(classToWrite){
				_.dbProperties.map { _.jsonPropName } }(_modelFromAssignments)
	}
}
