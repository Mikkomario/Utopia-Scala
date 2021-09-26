package utopia.vault.coder.controller.writer

import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.enumeration.PropertyType.EnumValue
import utopia.vault.coder.model.scala.Visibility.Public
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.{Code, Extension, Parameter, Reference}
import utopia.vault.coder.model.scala.declaration.{File, MethodDeclaration, ObjectDeclaration, PropertyDeclaration}

import scala.collection.immutable.VectorBuilder
import scala.io.Codec

/**
  * Used for writing standard model (from DB) factory objects
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object FactoryWriter
{
	/**
	  * Writes a factory used for processing database object data
	  * @param classToWrite Class data based on which the factory is created
	  * @param tablesRef Reference to the tables object
	  * @param modelRef Reference to the read model class
	  * @param dataRef Reference to the partial model data class
	  * @param codec Implicit codec to use when writing the document
	  * @param setup Implicit project-specific setup
	  * @return Reference to the new written factory object. Failure if writing failed.
	  */
	def apply(classToWrite: Class, tablesRef: Reference, modelRef: Reference, dataRef: Reference)
	         (implicit codec: Codec, setup: ProjectSetup) =
	{
		val parentPackage = setup.factoryPackage/classToWrite.packageName
		val objectName = s"${classToWrite.name}Factory"
		File(parentPackage,
			ObjectDeclaration(objectName, extensionsFor(classToWrite, modelRef),
				properties = propertiesFor(classToWrite, tablesRef),
				methods = methodsFor(classToWrite, modelRef, dataRef),
				description = s"Used for reading ${classToWrite.name} data from the DB"
			)
		).write()
	}
	
	private def extensionsFor(classToWrite: Class, modelRef: Reference): Vector[Extension] =
	{
		val builder = new VectorBuilder[Extension]()
		
		// If no enumerations are included, the inheritance is more specific (=> uses automatic validation)
		if (classToWrite.refersToEnumerations)
			builder += Reference.fromRowModelFactory(modelRef)
		else
			builder += Reference.fromValidatedRowModelFactory(modelRef)
		
		// For tables which contain a creation time index, additional inheritance is added
		if (classToWrite.recordsCreationTime)
			builder += Reference.fromRowFactoryWithTimestamps(modelRef)
		
		// If the class supports deprecation, it is reflected in this factory also
		if (classToWrite.isDeprecatable)
			builder += Reference.deprecatable
		
		builder.result()
	}
	
	private def propertiesFor(classToWrite: Class, tablesRef: Reference)(implicit setup: ProjectSetup) =
	{
		val builder = new VectorBuilder[PropertyDeclaration]()
		
		// All objects define the table property (implemented)
		builder += ComputedProperty("table", Set(tablesRef), isOverridden = true)(
			s"${tablesRef.target}.${classToWrite.name.singular.uncapitalize}")
		// Timestamp-based factories also specify a creation time property name
		classToWrite.creationTimeProperty.foreach { createdProp =>
			builder += ComputedProperty("creationTimePropertyName", isOverridden = true)(
				createdProp.name.singular.quoted)
		}
		// Deprecatable factories specify the deprecation condition (read from the database model)
		if (classToWrite.isDeprecatable)
		{
			val dbModelName = s"${classToWrite.name}Model"
			val dbModelRef = Reference(setup.dbModelPackage/classToWrite.packageName, dbModelName)
			builder += ComputedProperty("nonDeprecatedCondition", Set(dbModelRef), isOverridden = true)(
				s"$dbModelName.nonDeprecatedCondition")
		}
		
		builder.result()
	}
	
	private def methodsFor(classToWrite: Class, modelRef: Reference, dataRef: Reference) =
	{
		val applyMethod =
		{
			// Case: Enumerations are used => has to process enumeration values separately in custom apply method
			if (classToWrite.refersToEnumerations)
				new MethodDeclaration(Public, "apply",
					Parameter("model", Reference.templateModel(Reference.property)),
					enumAwareApplyCode(classToWrite, modelRef, dataRef), None, "", "",
					isOverridden = true)
			// Case: No enumerations are used => implements a simpler fromValidatedModel
			else
				MethodDeclaration("fromValidatedModel", Set(modelRef, dataRef, Reference.valueUnwraps),
					isOverridden = true)(Parameter("model", Reference.model(Reference.constant)))(
					s"${modelRef.target}(model(${"\"id\""}), ${dataRef.target}(${
						classToWrite.properties.map { prop =>
							s"model(${prop.name.singular.quoted})" }.mkString(", ")}))")
		}
		Set(applyMethod)
	}
	
	private def enumAwareApplyCode(classToWrite: Class, modelRef: Reference, dataRef: Reference) =
	{
		// Needs to validate the specified model
		val validationLine = s"table.validate(model).flatMap { valid => "
		// Divides the class properties into enumeration-based values and standard values
		val dividedProperties = classToWrite.properties.map { prop => prop.dataType match
		{
			case enumVal: EnumValue => Left(prop -> enumVal)
			case _ => Right(prop)
		} }
		val enumProperties = dividedProperties.flatMap { _.leftOption }
		// Non-nullable enum-based values need to be parsed separately, because they may prevent model parsing
		val requiredEnumProperties = enumProperties.filter { !_._2.isNullable }
		val enumDeclarationLines = requiredEnumProperties.zipWithIndex.map { case ((property, enumValue), index) =>
			// Uses flatMap if there remain more conditions, otherwise uses map
			val methodName = if (index < requiredEnumProperties.size - 1) "flatMap" else "map"
			s"${"\t" * (index + 1)}${enumValue.enumeration.name}.forId(valid(${
				property.name.singular.quoted}).getInt).$methodName { ${property.name} => "
		}
		val innerIndentCount = enumDeclarationLines.size + 1
		val innerIndent = "\t" * innerIndentCount
		// Stores nullable enum values to increase readability
		val nullableDeclarationLines = enumProperties.filter { _._2.isNullable }.map { case (prop, enumVal) =>
			s"${innerIndent}val ${prop.name} = valid(${prop.name.singular.quoted}).int.flatMap(${
				enumVal.enumeration.name}.findForId)"
		}
		val creationLineBase = s"${modelRef.target}(valid(${"id".quoted}), ${dataRef.target}("
		val creationLinePropertiesPart = dividedProperties.map {
			case Left((prop, _)) => prop.name.singular
			case Right(prop) => s"valid(${prop.name.singular.quoted})"
		}.mkString(", ")
		val creationLine = s"$innerIndent$creationLineBase$creationLinePropertiesPart))"
		// Some lines are included for closing brackets
		val closingLines = (0 until innerIndentCount).reverseIterator.map { indent => "\t" * indent + "}" }
		
		// Combines the lines together
		val allLines = (validationLine +: enumDeclarationLines) ++ (nullableDeclarationLines :+
			creationLine) ++ closingLines
		// References the enumerations used
		Code(allLines, enumProperties.map { _._2.enumeration.reference }.toSet ++
			Set(modelRef, dataRef, Reference.valueUnwraps))
	}
}
