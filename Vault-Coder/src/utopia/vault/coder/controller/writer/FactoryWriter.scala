package utopia.vault.coder.controller.writer

import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.CodeBuilder
import utopia.vault.coder.model.data.{Class, ProjectSetup, Property}
import utopia.vault.coder.model.enumeration.PropertyType.EnumValue
import utopia.vault.coder.model.scala.Visibility.Public
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.{Extension, Parameter, Reference}
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
				description = s"Used for reading ${classToWrite.name} data from the DB", author = classToWrite.author
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
			{
				val dataAssignments = classToWrite.properties
					.map { prop => prop.dataType.fromValueCode(s"model(${prop.name.singular.quoted})") }
				MethodDeclaration("fromValidatedModel",
					Set(modelRef, dataRef, Reference.valueUnwraps) ++ dataAssignments.flatMap { _.references },
					isOverridden = true)(Parameter("model", Reference.model(Reference.constant)))(
					s"${modelRef.target}(model(${"\"id\""}), ${dataRef.target}(${dataAssignments.mkString(", ")}))")
			}
		}
		Set(applyMethod)
	}
	
	private def enumAwareApplyCode(classToWrite: Class, modelRef: Reference, dataRef: Reference) =
	{
		// Divides the class properties into enumeration-based values and standard values
		val dividedProperties = classToWrite.properties.map { prop => prop.dataType match
		{
			case enumVal: EnumValue => Left(prop -> enumVal)
			case _ => Right(prop)
		} }
		val enumProperties = dividedProperties.flatMap { _.leftOption }
		// Non-nullable enum-based values need to be parsed separately, because they may prevent model parsing
		val requiredEnumProperties = enumProperties.filter { !_._2.isNullable }
		
		val builder = new CodeBuilder()
		
		// Needs to validate the specified model
		val validateMapMethod = if (requiredEnumProperties.isEmpty) "map" else "flatMap"
		builder += s"table.validate(model).$validateMapMethod { valid => "
		builder.indent()
		
		declareEnumerations(builder, requiredEnumProperties.dropRight(1), "flatMap")
		declareEnumerations(builder, requiredEnumProperties.lastOption, "map")
		val innerIndentCount = requiredEnumProperties.size + 1
		
		// Stores nullable enum values to increase readability
		enumProperties.filter { _._2.isNullable }.foreach { case (prop, enumVal) =>
			builder += s"val ${prop.name} = valid(${prop.name.singular.quoted}).int.flatMap(${
				enumVal.enumeration.name}.findForId)"
		}
		
		// Writes the instance creation now that the enum-based properties have been declared
		builder.appendPartial(s"${modelRef.target}(valid(${"id".quoted}), ${dataRef.target}(")
		builder.appendPartial(dividedProperties.map {
			case Left((prop, _)) => CodePiece(prop.name.singular)
			case Right(prop) => prop.dataType.fromValueCode(s"valid(${prop.name.singular.quoted})")
		}.reduceLeft { _.append(_, ", ") } + "))", allowLineSplit = true)
		
		// Closes open blocks
		(0 until innerIndentCount).foreach { _ => builder.closeBlock() }
		
		// References the enumerations used
		builder.result().referringTo(enumProperties.map { _._2.enumeration.reference }.toSet ++
			Set(modelRef, dataRef, Reference.valueUnwraps))
	}
	
	// NB: Indents for each declared enumeration
	private def declareEnumerations(builder: CodeBuilder, enumProps: Iterable[(Property, EnumValue)],
	                                mapMethod: String) =
		enumProps.foreach { case (prop, enumVal) =>
			builder += s"${enumVal.enumeration.name}.forId(valid(${
				prop.name.singular.quoted}).getInt).$mapMethod { ${prop.name} => "
			builder.indent()
		}
}
