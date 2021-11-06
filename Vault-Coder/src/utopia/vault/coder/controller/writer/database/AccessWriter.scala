package utopia.vault.coder.controller.writer.database

import utopia.flow.datastructure.immutable.Pair
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Name, ProjectSetup}
import utopia.vault.coder.model.scala.Visibility.{Private, Protected}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, DeclarationStart, File, MethodDeclaration, ObjectDeclaration, TraitDeclaration}
import utopia.vault.coder.model.scala.{DeclarationDate, Extension, Parameter, Parameters, Reference, ScalaType}

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
	def singleIdAccessNameFor(c: Class) = s"DbSingle${ c.name }"
	
	/**
	  * @param c     A class
	  * @param setup Project setup (implicit)
	  * @return Package that contains singular access points for that class
	  */
	def singleAccessPackageFor(c: Class)(implicit setup: ProjectSetup) = setup.singleAccessPackage / c.packageName
	
	/**
	  * @param c     A class
	  * @param setup Project setup (implicit)
	  * @return Reference to the access point for unique instances of that class based on their id
	  */
	def singleIdReferenceFor(c: Class)(implicit setup: ProjectSetup) =
		Reference(singleAccessPackageFor(c), singleIdAccessNameFor(c))
	
	/**
	  * Writes database access point objects and traits
	  * @param classToWrite          Class based on which these access points are written
	  * @param modelRef              Reference to the stored model class
	  * @param factoryRef            Reference to the from DB factory object
	  * @param dbModelRef            Reference to the database model class
	  * @param descriptionReferences References to the described model version + single description link access point +
	  *                              many description links access point, if applicable for this class
	  * @param codec                 Implicit codec to use when writing files
	  * @param setup                 Implicit project-specific setup to use
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
		val singleAccessPackage = singleAccessPackageFor(classToWrite)
		val uniqueAccessName = s"Unique${ classToWrite.name }Access"
		// Standard access point properties (factory, model & defaultOrdering)
		// are the same for both single and many model access points
		// (except that defaultOrdering is missing from non-distinct access points)
		val baseProperties = Vector(
			ComputedProperty("factory", Set(factoryRef), isOverridden = true)(factoryRef.target),
			ComputedProperty("model", Set(dbModelRef), Protected,
				description = "Factory used for constructing database the interaction models")(dbModelRef.target)
		)
		val pullIdCode = classToWrite.idType.nullable.fromValueCode(s"pullColumn(index)")
		// For classes that support deprecation, deprecate() -method is added for all traits
		// (implementation varies, however)
		// Option[Pair[method]], where first method is for individual access and second for many access
		val deprecationMethods = classToWrite.deprecationProperty.map { prop =>
			Pair(prop.name.singular, prop.name.plural).map { propName =>
				MethodDeclaration("deprecate", Set(Reference.now, Reference.valueConversions))(
					Parameters(Vector(Vector()), Vector(connectionParam)))(s"$propName = Now")
			}
		}
		File(singleAccessPackage,
			TraitDeclaration(uniqueAccessName,
				// Extends SingleRowModelAccess, DistinctModelAccess and Indexed
				Vector(Reference.singleRowModelAccess(modelRef),
					Reference.distinctModelAccess(modelRef, ScalaType.option(modelRef), Reference.value),
					Reference.indexed),
				// Provides computed accessors for individual properties
				baseProperties ++ classToWrite.properties.map { prop =>
					val pullCode = prop.dataType.nullable
						.fromValueCode(s"pullColumn(model.${ prop.name }Column)")
					ComputedProperty(prop.name.singular, pullCode.references,
						description = prop.description.notEmpty.getOrElse(s"The ${ prop.name } of this instance") +
							". None if no instance (or value) was found.", implicitParams = Vector(connectionParam))(
						pullCode.text)
				} :+ ComputedProperty("id", pullIdCode.references, implicitParams = Vector(connectionParam))(
					pullIdCode.text),
				// Contains setters for each property (singular)
				propertySettersFor(classToWrite, connectionParam) { _.singular } ++ deprecationMethods.map { _.first },
				description = s"A common trait for access points that return individual and distinct ${
					classToWrite.name.plural
				}.", author = classToWrite.author
			)
		).write().flatMap { uniqueAccessRef =>
			// Writes the single model by id access point
			// This access point is used for accessing individual items based on their id
			// The inherited trait depends on whether descriptions are supported,
			// this also affects implemented properties
			val (singleIdAccessParent, singleIdAccessParentProperties) = descriptionReferences match {
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
					if (classToWrite.useLongId) {
						val idValueCode = classToWrite.idType.toValueCode("id")
						Extension(Reference.singleIdModelAccess(modelRef)) -> Vector(
							ComputedProperty("idValue", idValueCode.references, isOverridden = true)(idValueCode.text))
					}
					else
						Extension(Reference.singleIntIdModelAccess(modelRef)) -> Vector()
			}
			File(singleAccessPackage,
				ClassDeclaration(singleIdAccessNameFor(classToWrite),
					Vector(Parameter("id", classToWrite.idType.toScala)),
					Vector(uniqueAccessRef, singleIdAccessParent),
					// Implements the .condition property
					properties = singleIdAccessParentProperties,
					description = s"An access point to individual ${ classToWrite.name.plural }, based on their id",
					author = classToWrite.author, since = DeclarationDate.versionedToday, isCaseClass = true
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
					ObjectDeclaration(s"Db${ classToWrite.name }",
						Vector(Reference.singleRowModelAccess(modelRef), rootViewExtension, Reference.indexed),
						properties = baseProperties,
						// Defines an .apply(id) method for accessing individual items
						methods = Set(MethodDeclaration("apply", Set(singleIdAccessRef),
							returnDescription = s"An access point to that ${ classToWrite.name }")(
							Parameter("id", classToWrite.idType.toScala,
								description = s"Database id of the targeted ${ classToWrite.name } instance"))(
							s"${ singleIdAccessRef.target }(id)")),
						description = s"Used for accessing individual ${ classToWrite.name.plural }",
						author = classToWrite.author
					)
				).write().flatMap { singleAccessRef =>
					// Writes a trait common for the many model access points
					val manyAccessPackage = setup.manyAccessPackage / classToWrite.packageName
					val manyAccessTraitName = s"Many${ classToWrite.name.plural }Access"
					val subViewName = s"Many${ classToWrite.name.plural }SubView"
					val traitType = ScalaType.basic(manyAccessTraitName)
					
					// Trait parent type depends on whether descriptions are used or not
					val (manyTraitParent, manyParentProperties, manyParentMethods) = descriptionReferences match {
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
									Parameter("parent", Reference.manyRowModelAccess(modelRef),
										prefix = Some(DeclarationStart.overrideVal)),
									Parameter("filterCondition", Reference.condition,
										prefix = Some(DeclarationStart.overrideVal))),
								Vector(traitType, Reference.subView),
								visibility = Private
							)
						)),
						TraitDeclaration(manyAccessTraitName,
							Vector(Reference.manyRowModelAccess(modelRef), manyTraitParent),
							// Contains computed properties to access class properties
							baseProperties ++ classToWrite.properties.map { prop =>
								val pullCode = prop.dataType
									.fromValuesCode(s"pullColumn(model.${ prop.name }Column)")
								ComputedProperty(prop.name.plural, pullCode.references,
									description = s"${ prop.name.plural } of the accessible ${ classToWrite.name.plural }",
									implicitParams = Vector(connectionParam))(pullCode.text)
							} ++ Vector(
								ComputedProperty("ids", implicitParams = Vector(connectionParam))(
									s"pullColumn(index).flatMap { id => ${
										classToWrite.idType.nullable.fromValueCode("id")
									} }"),
								ComputedProperty("defaultOrdering", Set(factoryRef), Protected, isOverridden = true)(
									if (classToWrite.recordsIndexedCreationTime) "Some(factory.defaultOrdering)" else "None")
							) ++ manyParentProperties,
							// Contains setters for property values (plural)
							propertySettersFor(classToWrite, connectionParam) { _.plural } ++ manyParentMethods ++
								deprecationMethods.map { _.second } +
								MethodDeclaration("filter",
									explicitOutputType = Some(ScalaType.basic(manyAccessTraitName)),
									isOverridden = true)(
									Parameter("additionalCondition", Reference.condition))(
									s"new $manyAccessTraitName.$subViewName(this, additionalCondition)"),
							description = s"A common trait for access points which target multiple ${
								classToWrite.name.plural
							} at a time", author = classToWrite.author
						)
					).write().flatMap { manyAccessTraitRef =>
						// Writes the many model access point
						val manyAccessName =
						{
							if (classToWrite.name.singular == classToWrite.name.plural)
								s"DbMany${classToWrite.name.plural}"
							else
								s"Db${ classToWrite.name.plural }"
						}
						// There is also a nested object for id-based multi-access, which may have description support
						val subsetClassName = s"Db${ classToWrite.name.plural }Subset"
						val subSetClass = descriptionReferences match
						{
							case Some((describedRef, _, _)) =>
								ClassDeclaration(subsetClassName,
									Parameter("ids", ScalaType.set(ScalaType.int),
										prefix = Some(DeclarationStart.overrideVal)),
									Vector(manyAccessTraitRef,
										Reference.manyDescribedAccessByIds(modelRef, describedRef))
								)
							case None =>
								ClassDeclaration(subsetClassName,
									Parameter("targetIds", ScalaType.set(ScalaType.int)),
									Vector(manyAccessTraitRef),
									properties = Vector(ComputedProperty("globalCondition",
										Set(Reference.valueConversions, Reference.sqlExtensions), isOverridden = true)(
										"Some(index in targetIds)"))
								)
						}
						val subSetClassAccessMethod = MethodDeclaration("apply",
							returnDescription = s"An access point to ${
								classToWrite.name.plural
							} with the specified ids")(
							Parameter("ids", ScalaType.set(ScalaType.int),
								description = s"Ids of the targeted ${ classToWrite.name.plural }"))(
							s"new $subsetClassName(ids)")
						File(manyAccessPackage,
							ObjectDeclaration(manyAccessName, Vector(manyAccessTraitRef, rootViewExtension),
								methods = Set(subSetClassAccessMethod),
								nested = Set(subSetClass),
								description = s"The root access point when targeting multiple ${
									classToWrite.name.plural
								} at a time", author = classToWrite.author
							)
						).write().map { manyAccessRef =>
							(uniqueAccessRef, singleAccessRef, manyAccessTraitRef, manyAccessRef)
						}
					}
				}
			}
		}
	}
	
	private def propertySettersFor(classToWrite: Class, connectionParam: Parameter)(nameFromPropName: Name => String) =
		classToWrite.properties.map { prop =>
			val paramName = s"new${ prop.name.singular.capitalize }"
			val paramType = prop.dataType.notNull
			val valueConversionCode = paramType.toValueCode(paramName)
			MethodDeclaration(s"${ nameFromPropName(prop.name) }_=", valueConversionCode.references,
				description = s"Updates the ${ prop.name } of the targeted ${ classToWrite.name } instance(s)",
				returnDescription = s"Whether any ${ classToWrite.name } instance was affected")(
				Parameter(paramName, paramType.toScala, description = s"A new ${ prop.name } to assign")
					.withImplicits(connectionParam))(s"putColumn(model.${ prop.name }Column, $valueConversionCode)")
		}.toSet
}
