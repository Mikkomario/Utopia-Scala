package utopia.vault.coder.controller.writer

import utopia.vault.coder.model.scala.Visibility.{Private, Protected}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.scala.{Extension, Parameter, Parameters, Reference, ScalaType}
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration, TraitDeclaration}

import scala.io.Codec

/**
  * Used for writing database access templates
  * @author Mikko Hilpinen
  * @since 2.9.2021, v0.1
  */
object AccessWriter
{
	/**
	  * @param c A class
	  * @return Name of the single id access point for that class
	  */
	def singleIdAccessNameFor(c: Class) = s"DbSingle${c.name}"
	/**
	  * @param c A class
	  * @param setup Project setup (implicit)
	  * @return Package that contains singular access points for that class
	  */
	def singleAccessPackageFor(c: Class)(implicit setup: ProjectSetup) = setup.singleAccessPackage/c.packageName
	/**
	  * @param c A class
	  * @param setup Project setup (implicit)
	  * @return Reference to the access point for unique instances of that class based on their id
	  */
	def singleIdReferenceFor(c: Class)(implicit setup: ProjectSetup) =
		Reference(singleAccessPackageFor(c), singleIdAccessNameFor(c))
	
	/**
	  * Writes database access point objects and traits
	  * @param classToWrite Class based on which these access points are written
	  * @param modelRef Reference to the stored model class
	  * @param factoryRef Reference to the from DB factory object
	  * @param dbModelRef Reference to the database model class
	  * @param descriptionReferences References to the described model version + single description link access point +
	  *                            many description links access point, if applicable for this class
	  * @param codec Implicit codec to use when writing files
	  * @param setup Implicit project-specific setup to use
	  * @return References to<br>
	  *         1) trait common for distinct single model access points<br>
	  *         2) Single model access point object<br>
	  *         3) Trait common for many model access points<br>
	  *         4) Many model access point object
	  */
	def apply(classToWrite: Class, modelRef: Reference, factoryRef: Reference, dbModelRef: Reference,
	          descriptionReferences: Option[(Reference, Reference, Reference)])
	         (implicit codec: Codec, setup: ProjectSetup) =
	{
		val connectionParam = Parameter("connection", Reference.connection)
		// Writes a trait common for unique model access points
		val singleAccessPackage =  singleAccessPackageFor(classToWrite)
		val uniqueAccessName = s"Unique${classToWrite.name}Access"
		// Standard access point properties (factory, model & defaultOrdering)
		// are the same for both single and many model access points
		// (except that defaultOrdering is missing from non-distinct access points)
		val baseProperties = Vector(
			ComputedProperty("factory", Set(factoryRef), isOverridden = true)(factoryRef.target),
			ComputedProperty("model", Set(dbModelRef), Protected,
				description = "Factory used for constructing database the interaction models")(dbModelRef.target)
		)
		// Property setters are common for both distinct access points (unique & many)
		val propertySetters = classToWrite.properties.map { prop =>
			val paramName = s"new${prop.name.singular.capitalize}"
			val paramType = prop.dataType.notNull
			val valueConversionCode = paramType.toValueCode(paramName)
			MethodDeclaration(s"${prop.name}_=", valueConversionCode.references,
				description = s"Updates the ${prop.name} of the targeted ${classToWrite.name} instance(s)",
				returnDescription = s"Whether any ${classToWrite.name} instance was affected")(
				Parameter(paramName, paramType.toScala, description = s"A new ${prop.name} to assign")
					.withImplicits(connectionParam))(s"putColumn(model.${prop.name}Column, $valueConversionCode)")
		}.toSet
		val pullIdCode = classToWrite.idType.nullable.fromValueCode(s"pullColumn(index)")
		File(singleAccessPackage,
			TraitDeclaration(uniqueAccessName,
				// Extends SingleRowModelAccess, DistinctModelAccess and Indexed
				Vector(Reference.singleRowModelAccess(modelRef),
					Reference.distinctModelAccess(modelRef, ScalaType.option(modelRef), Reference.value),
					Reference.indexed),
				// Provides computed accessors for individual properties
				baseProperties ++ classToWrite.properties.map { prop =>
					val pullCode = prop.dataType.nullable
						.fromValueCode(s"pullColumn(model.${prop.name}Column)")
					ComputedProperty(prop.name.singular, pullCode.references,
						description = prop.description.notEmpty.getOrElse(s"The ${prop.name} of this instance") +
							". None if no instance (or value) was found.", implicitParams = Vector(connectionParam))(
						pullCode.text)
				} :+ ComputedProperty("id", pullIdCode.references, implicitParams = Vector(connectionParam))(
					pullIdCode.text),
				propertySetters,
				description = s"A common trait for access points that return individual and distinct ${
					classToWrite.name.plural}.", author = classToWrite.author
			)
		).write().flatMap { uniqueAccessRef =>
			// Writes the single model by id access point
			// This access point is used for accessing individual items based on their id
			// The inherited trait depends on whether descriptions are supported,
			// this also affects implemented properties
			val (singleIdAccessParent, singleIdAccessParentProperties) = descriptionReferences match
			{
				// Case: Class uses description => extends described access version with its propeties
				case Some((describedRef, singleDescRef, manyDescsRef)) =>
					val props = Vector(
						ComputedProperty("singleDescriptionAccess", Set(singleDescRef), Protected,
							isOverridden = true)(singleDescRef.target),
						ComputedProperty("manyDescriptionsAccess", Set(manyDescsRef), Protected, isOverridden = true)(
							manyDescsRef.target),
						ComputedProperty("describedFactory", Set(describedRef), Protected, isOverridden = true)(
							describedRef.target)
					)
					Extension(Reference.singleIdDescribedAccess(modelRef, describedRef)) -> props
				// Case: Descriptions are not supported => Extends SingleIdModel or its easier sub-trait
				case None =>
					if (classToWrite.useLongId)
					{
						val idValueCode = classToWrite.idType.toValueCode("id")
						Extension(Reference.singleIdModelAccess) -> Vector(
							ComputedProperty("idValue", idValueCode.references, isOverridden = true)(idValueCode.text))
					}
					else
						Extension(Reference.singleIntIdModelAccess) -> Vector()
			}
			File(singleAccessPackage,
				ClassDeclaration(singleIdAccessNameFor(classToWrite),
					Vector(Parameter("id", classToWrite.idType.toScala)),
					Vector(uniqueAccessRef, singleIdAccessParent),
					// Implements the .condition property
					properties = singleIdAccessParentProperties,
					description = s"An access point to individual ${classToWrite.name.plural}, based on their id",
					isCaseClass = true
				)
			).write().flatMap { singleIdAccessRef =>
				// Writes the single model access point
				// Root access points extend either the UnconditionalView or the NonDeprecatedView -trait,
				// depending on whether deprecation is supported
				val rootViewExtension: Extension = {
					if (classToWrite.isDeprecatable)
						Reference.nonDeprecatedView(modelRef)
					else
						Reference.unconditionalView
				}
				File(singleAccessPackage,
					ObjectDeclaration(s"Db${classToWrite.name}",
						Vector(Reference.singleRowModelAccess(modelRef), rootViewExtension, Reference.indexed),
						properties = baseProperties,
						// Defines an .apply(id) method for accessing individual items
						methods = Set(MethodDeclaration("apply", Set(singleIdAccessRef),
							returnDescription = s"An access point to that ${classToWrite.name}")(
							Parameter("id", classToWrite.idType.toScala,
								description = s"Database id of the targeted ${classToWrite.name} instance"))(
							s"${singleIdAccessRef.target}(id)")),
						description = s"Used for accessing individual ${classToWrite.name.plural}",
						author = classToWrite.author
					)
				).write().flatMap { singleAccessRef =>
					// Writes a trait common for the many model access points
					val manyAccessPackage =  setup.manyAccessPackage/classToWrite.packageName
					val manyAccessTraitName = s"Many${classToWrite.name.plural}Access"
					val subViewName = s"Many${classToWrite.name.plural}SubView"
					val traitType = ScalaType.basic(manyAccessTraitName)
					
					// Trait parent type depends on whether descriptions are used or not
					val (manyTraitParent, manyParentProperties, manyParentMethods) = descriptionReferences match
					{
						case Some((describedRef, _, manyDescsRef)) =>
							(Extension(Reference.manyDescribedAccess(modelRef, describedRef)), Vector(
								ComputedProperty("manyDescriptionsAccess", Set(manyDescsRef), Protected,
									isOverridden = true)(manyDescsRef.target),
								ComputedProperty("describedFactory", Set(describedRef), Protected,
									isOverridden = true)(describedRef.target)
							), Set(MethodDeclaration("idOf", isOverridden = true)(Parameter("item", modelRef))(
								"item.id")))
						case None => (Extension(Reference.indexed), Vector(), Set[MethodDeclaration]())
					}
					File(manyAccessPackage,
						ObjectDeclaration(manyAccessTraitName, nested = Set(
							ClassDeclaration(subViewName,
								Parameters(
									Parameter("parent", Reference.manyRowModelAccess(modelRef), prefix = "override val"),
									Parameter("filterCondition", Reference.condition, prefix = "override val")),
								Vector(traitType, Reference.subView),
								visibility = Private
							)
						)),
						TraitDeclaration(manyAccessTraitName,
							Vector(Reference.manyRowModelAccess(modelRef), manyTraitParent),
							// Contains computed properties to access class properties
							baseProperties ++ classToWrite.properties.map { prop =>
								ComputedProperty(prop.name.plural,
									description = s"${prop.name.plural} of the accessible ${classToWrite.name.plural}",
									implicitParams = Vector(connectionParam))(
									s"pullColumn(model.${prop.name}Column).flatMap { value => ${
										prop.dataType.nullable.fromValueCode("value")} }")
							} ++ Vector(
								ComputedProperty("ids", implicitParams = Vector(connectionParam))(
									s"pullColumn(index).flatMap { id => ${
										classToWrite.idType.nullable.fromValueCode("id")} }"),
								ComputedProperty("defaultOrdering", Set(factoryRef), Protected, isOverridden = true)(
									if (classToWrite.recordsIndexedCreationTime) "Some(factory.defaultOrdering)" else "None")
							) ++ manyParentProperties,
							propertySetters ++ manyParentMethods + MethodDeclaration("filter", isOverridden = true)(
								Parameter("additionalCondition", Reference.condition))(
								s"new $subViewName(additionalCondition)"),
							description = s"A common trait for access points which target multiple ${
								classToWrite.name.plural} at a time", author = classToWrite.author
						)
					).write().flatMap { manyAccessTraitRef =>
						// Writes the many model access point
						val manyAccessName = s"Db${classToWrite.name.plural}"
						// Classes that support descriptions also get a nested object for id-based multi-access
						val manyByIdsAccess = descriptionReferences.map { case (describedRef, _, _) =>
							val subsetClassName = s"Db${classToWrite.name.plural}Subset"
							ClassDeclaration(subsetClassName,
								Parameter("ids", ScalaType.set(ScalaType.int), prefix = "override val"),
								Vector(manyAccessTraitRef,
									Reference.manyDescribedAccessByIds(modelRef, describedRef))
							) -> MethodDeclaration("apply",
								returnDescription = s"An access point to ${
									classToWrite.name.plural} with the specified ids")(
								Parameter("ids", ScalaType.set(ScalaType.int),
									description = s"Ids of the targeted ${classToWrite.name.plural}"))(
								s"new $subsetClassName(ids)")
						}
						File(manyAccessPackage,
							ObjectDeclaration(manyAccessName, Vector(manyAccessTraitRef, rootViewExtension),
								methods = manyByIdsAccess.map { _._2 }.toSet,
								nested = manyByIdsAccess.map { _._1 }.toSet,
								description = s"The root access point when targeting multiple ${
									classToWrite.name.plural} at a time", author = classToWrite.author
							)
						).write().map { manyAccessRef =>
							(uniqueAccessRef, singleAccessRef, manyAccessTraitRef, manyAccessRef)
						}
					}
				}
			}
		}
	}
}
