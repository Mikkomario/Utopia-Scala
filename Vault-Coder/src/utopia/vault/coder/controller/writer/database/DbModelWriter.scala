package utopia.vault.coder.controller.writer.database

import utopia.coder.model.data
import utopia.coder.model.data.{Name, Named, NamingRules}
import utopia.coder.model.enumeration.NamingConvention.CamelCase
import utopia.coder.model.scala.Visibility.Protected
import utopia.coder.model.scala.datatype.Reference._
import utopia.coder.model.scala.datatype.{Extension, Reference, ScalaType}
import utopia.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue, LazyValue}
import utopia.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration, PropertyDeclaration}
import utopia.coder.model.scala.{DeclarationDate, Parameter}
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, DbProperty, Property, VaultProjectSetup}
import utopia.vault.coder.util.VaultReferences.Vault._
import utopia.vault.coder.util.VaultReferences._

import scala.io.Codec

/**
  * Used for writing database model scala files
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object DbModelWriter
{
	// ATTRIBUTES   -------------------------------------
	
	/**
	  * Suffix added to class name in order to make it a database model class name
	  */
	val classNameSuffix = data.Name("Model", "Models", CamelCase.capitalized)
	
	private val withMethodPrefix = data.Name("with", "with", CamelCase.lower)
	
	
	// OTHER    -----------------------------------------
	
	/**
	  * Generates the DB model class and the associated companion object
	  * @param classToWrite The base class
	  * @param modelRef     Reference to the stored model class
	  * @param dataRef      Reference to the data class
	  * @param factoryRef   Reference to the factory trait for copy-constructing
	  * @param dbFactoryRef   Reference to the factory class
	  * @param codec        Implicit codec used when writing the file
	  * @param setup        Target project -specific setup (implicit)
	  * @return Reference to the generated class. Failure if writing failed.
	  */
	def apply(classToWrite: Class, modelRef: Reference, dataRef: Reference, factoryRef: Reference,
	          dbFactoryRef: Reference)
	         (implicit codec: Codec, setup: VaultProjectSetup, naming: NamingRules) =
	{
		val parentPackage = setup.dbModelPackage / classToWrite.packageName
		val className = (classToWrite.name + classNameSuffix).className
		val classType = ScalaType.basic(className)
		val deprecation = DeprecationStyle.of(classToWrite)
		
		val optionalIdType = classToWrite.idType.optional
		
		// Converts each property to the "intermediate" state
		val applyParametersCode = ("None" +: classToWrite.properties.flatMap { prop =>
			val propAccessCode = s"data.${prop.name.prop}"
			prop.dataType.sqlConversions
				.map { conversion => conversion.midConversion(propAccessCode) }
		}).mkString(", ")
		
		// The generated file contains the model class and the associated companion object
		File(parentPackage,
			ObjectDeclaration(className,
				factoryExtensionsFor(className, modelRef, dataRef, factoryRef, deprecation),
				// Contains an access property for each property, as well as factory and table -properties
				properties = classToWrite.dbProperties.map { prop =>
					LazyValue(prop.name.prop,
						description = s"Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }")(
						s"property(${ prop.modelName.quoted })")
				}.toVector ++ Vector(
					ComputedProperty("factory", Set(dbFactoryRef),
						description = "The factory object used by this model type")(dbFactoryRef.target),
					ComputedProperty("table", isOverridden = true)("factory.table")
				) ++ deprecation.iterator.flatMap { _.properties },
				// Implements .apply(...) and .complete(id, data)
				methods = Set(
					MethodDeclaration("apply", isOverridden = true)(Parameter("data", dataRef))(
						s"apply($applyParametersCode)"),
					MethodDeclaration("complete", Set(modelRef), visibility = Protected, isOverridden = true)(
						Vector(Parameter("id", flow.value), Parameter("data", dataRef)))(
						s"${ modelRef.target }(id.get${ if (classToWrite.useLongId) "Long" else "Int" }, data)"),
					MethodDeclaration("withId", isOverridden = true)(
						Parameter("id", classToWrite.idType.toScala))(
						"apply(Some(id))")
					// Also includes withX(...) methods for each property
				) ++ classToWrite.properties.flatMap { withPropertyMethods(_) } ++
					deprecation.iterator.flatMap { _.methods },
				description = s"Used for constructing $className instances and for inserting ${
					classToWrite.name.pluralDoc
				} to the database", author = classToWrite.author, since = DeclarationDate.versionedToday
			),
			ClassDeclaration(className,
				// Accepts a copy of all properties where each appears in the "intermediate "(db property) state
				constructionParams = Parameter("id", optionalIdType.toScala, optionalIdType.emptyValue,
					description = s"${ classToWrite.name.doc } database id") +:
					classToWrite.dbProperties.map { prop =>
						val inputType = prop.conversion.intermediate
						val defaultValue = inputType.emptyValue
						// TODO: Parameter descriptions are missing
						Parameter(prop.name.prop, inputType.scalaType, defaultValue)
					}.toVector,
				// Extends StorableWithFactory[A] with the factory traits
				extensions = Vector(storableWithFactory(modelRef), factoryRef(classType),
					fromIdFactory(ScalaType.int, classType)),
				// Implements the required properties: factory & valueProperties
				properties = Vector(
					ComputedProperty("factory", isOverridden = true)(s"$className.factory"),
					valuePropertiesPropertyFor(classToWrite, className)
				),
				// adds withX(...) -methods for convenience
				methods = classToWrite.properties.flatMap { withPropertyMethods(_, "copy",
					"A new copy of this model with the specified ") }.toSet +
					MethodDeclaration("withId", isOverridden = true)(
						Parameter("id", classToWrite.idType.toScala))("copy(id = Some(id))"),
				description = s"Used for interacting with ${ classToWrite.name.plural } in the database",
				author = classToWrite.author, since = DeclarationDate.versionedToday, isCaseClass = true)
		).write()
	}
	
	private def factoryExtensionsFor(className: String, modelRef: Reference, dataRef: Reference, factoryRef: Reference,
	                                 deprecation: Option[DeprecationStyle]): Vector[Extension] =
	{
		// The class itself doesn't need to be imported (same file)
		val classType = ScalaType.basic(className)
		// All factories extend the StorableFactory trait, the factory trait and FromIdFactory trait
		val baseExtensions = Vector[Extension](
			vault.storableFactory(classType, modelRef, dataRef),
			factoryRef(modelRef),
			fromIdFactory(modelRef)
		)
		// They may also extend a deprecation-related trait
		deprecation match {
			case Some(deprecation) => baseExtensions :+ deprecation.extensionFor(classType)
			case None => baseExtensions
		}
	}
	
	private def valuePropertiesPropertyFor(classToWrite: Class, className: String)
	                                      (implicit naming: NamingRules) =
	{
		val quotedId = classToWrite.idDatabasePropName.quoted
		if (classToWrite.properties.isEmpty)
			ComputedProperty("valueProperties", Set(flow.valueConversions), isOverridden = true)(
				s"Vector($quotedId -> id)"
			)
		else {
			val propsPart = classToWrite.dbProperties
				.map { prop => prop.toValueCode.withPrefix(s"$className.${ prop.name.prop }.name -> ") }
				.reduceLeft { _.append(_, ", ") }
			ComputedProperty("valueProperties", propsPart.references + flow.valueConversions, isOverridden = true)(
				s"Vector($quotedId -> id, $propsPart)"
			)
		}
	}
	
	private def withMethodNameFor(prop: Named)(implicit naming: NamingRules): String = withMethodNameFor(prop.name)
	private def withMethodNameFor(name: Name)(implicit naming: NamingRules) = (withMethodPrefix + name).prop
	
	private def withPropertyMethods(property: Property, calledMethodName: String = "apply",
	                                returnDescriptionStart: String = "A model containing only the specified ")
	                               (implicit naming: NamingRules) =
	{
		val concreteProp = property.concrete
		concreteProp.oneOrManyDbVariants match {
			// Case: The property matches a single column => generates one withX -method
			case Left(dbProp) =>
				Vector(withDbPropertyMethod(dbProp, concreteProp.description, calledMethodName = calledMethodName))
			// Case: The property matches multiple columns => generates partial and full withX method
			// variants
			case Right(dbProps) =>
				val extraDescription = s", which is part of the property ${ concreteProp.name }"
				val partMethods = dbProps.map {
					// NB: The accepted parameter type may be incorrect
					withDbPropertyMethod(_, returnDescriptionAppendix = extraDescription,
						calledMethodName = calledMethodName, returnDescriptionStart = returnDescriptionStart)
				}
				partMethods :+ withMethod(concreteProp, dbProps, concreteProp.dataType.toScala, concreteProp.description,
					s" (sets all ${dbProps.size} values)", calledMethodName, returnDescriptionStart)
		}
	}
	private def withDbPropertyMethod(property: DbProperty, paramDescription: String = "",
	                                 returnDescriptionAppendix: String = "", calledMethodName: String = "apply",
	                                 returnDescriptionStart: String = "A model containing only the specified ")
	                                (implicit naming: NamingRules) =
		withMethod(property, Vector(property), property.conversion.origin, paramDescription, returnDescriptionAppendix,
			calledMethodName, returnDescriptionStart)
	private def withMethod(source: Named, properties: Seq[DbProperty], parameterType: ScalaType,
	                       paramDescription: String = "", returnDescriptionAppendix: String = "",
	                       calledMethodName: String = "apply",
	                       returnDescriptionStart: String = "A model containing only the specified ")
	                      (implicit naming: NamingRules) =
	{
		val paramName = source.name.prop
		val constructionParamsCode = properties
			.map { prop => s"${prop.name.prop} = " +: prop.conversion.midConversion(paramName) }
			.reduceLeft { _.append(_, ", ") }
		MethodDeclaration(withMethodNameFor(source), constructionParamsCode.references,
			returnDescription = s"$returnDescriptionStart${ source.name.doc }$returnDescriptionAppendix",
			isLowMergePriority = true)(
			Parameter(paramName, parameterType, description = paramDescription))(
			s"$calledMethodName($constructionParamsCode)")
	}
	
	
	// NESTED   --------------------------------------
	
	private sealed trait DeprecationStyle
	{
		def extensionFor(dbModelClass: ScalaType): Extension
		
		def properties(implicit naming: NamingRules): Vector[PropertyDeclaration]
		
		def methods: Set[MethodDeclaration]
	}
	
	private object DeprecationStyle
	{
		def of(c: Class) =
			c.deprecationProperty
				.map { deprecationProp =>
					c.expirationProperty match {
						case Some(expirationProp) =>
							CombinedDeprecation(expirationProp.name, deprecationProp.name)
						case None => NullDeprecates(deprecationProp)
					}
				}
				.orElse { c.expirationProperty.map { prop => Expires(prop) } }
	}
	
	private case class NullDeprecates(prop: Property) extends DeprecationStyle
	{
		// ATTRIBUTES   -----------------------------
		
		private val camelPropName = prop.name.to(CamelCase.lower).singular
		// Whether this deprecation matches the expected default
		val isDefault = camelPropName == "deprecatedAfter"
		
		
		// IMPLEMENTED  -----------------------------
		
		override def extensionFor(dbModelClass: ScalaType) =
			(if (isDefault) deprecatableAfter else nullDeprecatable)(dbModelClass)
		
		override def properties(implicit naming: NamingRules) =
			if (isDefault) Vector() else Vector(
				ImmutableValue("deprecationAttName", isOverridden = true)(prop.dbProperties.head.modelName.quoted))
		// withDeprecatedAfter(...) must be provided separately for custom property names
		override def methods = if (isDefault) Set() else Set(
			MethodDeclaration("withDeprecatedAfter", isOverridden = true)(
				Parameter("deprecationTime", instant))(s"with${ camelPropName.capitalize }(deprecationTime)")
		)
	}
	
	private case class Expires(prop: Property) extends DeprecationStyle
	{
		override def extensionFor(dbModelClass: ScalaType) = expiring
		
		override def properties(implicit naming: NamingRules) =
			Vector(ImmutableValue("deprecationAttName", isOverridden = true)(prop.dbProperties.head.modelName.quoted))
		override def methods = Set()
	}
	
	private case class CombinedDeprecation(expirationPropName: Name, deprecationPropName: Name)
		extends DeprecationStyle
	{
		override def extensionFor(dbModelClass: ScalaType) = deprecatable
		
		override def properties(implicit naming: NamingRules) = Vector(
			ComputedProperty("nonDeprecatedCondition", Set(flow.valueConversions, flow.now),
				isOverridden = true)(
				s"${deprecationPropName.prop}.column.isNull && ${expirationPropName.prop}.column > Now")
		)
		override def methods = Set()
	}
}
