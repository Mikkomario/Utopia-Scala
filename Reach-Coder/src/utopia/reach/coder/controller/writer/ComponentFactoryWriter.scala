package utopia.reach.coder.controller.writer

import utopia.coder.model.data.{Name, NamingRules, ProjectSetup}
import utopia.coder.model.scala.Visibility.Protected
import utopia.coder.model.scala.datatype.TypeVariance.Covariance
import utopia.coder.model.scala.datatype.{Extension, GenericType, Reference, ScalaType}
import utopia.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue}
import utopia.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration, PropertyDeclaration, TraitDeclaration}
import utopia.coder.model.scala.{DeclarationDate, Parameter}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.reach.coder.model.data.{ComponentFactory, Property}
import utopia.reach.coder.model.enumeration.ContextType
import utopia.reach.coder.util.ReachReferences.Reach._
import Reference._

/**
  * Used for writing component factory classes
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.0
  */
// TODO: Getters should be public and not protected
object ComponentFactoryWriter
{
	// ATTRIBUTES -----------------------
	
	private lazy val repr = GenericType("Repr", variance = Covariance,
		description = "Implementing factory/settings type")
	private lazy val reprType = repr.toScalaType
	
	
	// OTHER    -----------------------
	
	/**
	  * Writes component creation factory traits, classes and objects
	  * @param factory Factory definitions
	  * @param naming Naming logic used
	  * @param setup Implicit project settings
	  * @return Reference to the generated file. Failure if writing failed.
	  */
	def apply(factory: ComponentFactory)(implicit naming: NamingRules, setup: ProjectSetup) = {
		val settingsName = (factory.componentName + "Settings").className
		
		val settingsLikeDec = settingsLike(factory)
		val settingsLikeType = settingsLikeDec.toBasicType
		
		val settingsCompanionDec = settingsCompanion(settingsName)
		val settingsDec = settings(factory, settingsName, settingsLikeType)
		val settingsType = settingsDec.toBasicType
		
		val settingsWrapperDec = settingsWrapper(factory, settingsLikeType, settingsType)
		val settingsWrapperType = settingsWrapperDec.toBasicType
		
		val factoryLikeDec = factoryLike(factory, settingsWrapperType)
		val factoryLikeType = factoryLikeDec.toBasicType
		
		val contextualDec = factory.contextType.map { context =>
			contextualFactory(factory, ("Contextual" +: (factory.componentName + "Factory")).className, context,
				settingsType, factoryLikeType) -> context
		}
		val nonContextualDec = {
			if (factory.onlyContextual)
				None
			else
				Some(nonContextualFactory(factory, settingsType, factoryLikeType,
					contextualDec.map { case (contextual, context) => contextual.toBasicType -> context }))
		}
		
		val setupDec = {
			if (nonContextualDec.isEmpty && contextualDec.isEmpty)
				None
			else
				Some(setupFactory(factory, settingsType, settingsWrapperType, nonContextualDec.map { _.toBasicType },
					contextualDec.map { case (contextual, context) => (contextual.toBasicType, context) }))
		}
		val companionDec = setupDec.map { setupDec => companion(factory, settingsType, setupDec.toBasicType) }
		
		File(
			factory.pck,
			Vector(settingsLikeDec, settingsCompanionDec, settingsDec, settingsWrapperDec, factoryLikeDec) ++
				contextualDec.map { _._1 } ++ nonContextualDec ++ setupDec ++ companionDec,
			factory.componentName.className, Set[Reference]()
		).write()
	}
	
	// Declares the generic settings trait
	private def settingsLike(factory: ComponentFactory)(implicit naming: NamingRules, setup: ProjectSetup) = {
		// Adds additional properties and methods for nested settings
		val (referencedProps, referencedMethods) = factory.properties.splitFlatMap { prop =>
			prop.reference.iterator.splitFlatMap { case (factory, prefix) =>
				referencedPropFunctionsFrom(prop, factory, prefix)
			}
		}
		TraitDeclaration(
			name = (factory.componentName + "SettingsLike").className,
			genericTypes = Vector(repr),
			// Allows custom extensions
			extensions = factory.parentTraits.map { _.toExtension(reprType) },
			// Defines abstract properties
			properties = factory.properties.map { prop =>
				PropertyDeclaration.newAbstract(prop.name.function, prop.dataType, description = prop.description,
					isProtected = true)
			} ++ referencedProps,
			// Defines abstract property setters, as well as mapping functions based on each setter
			methods = factory.properties.flatMap { prop =>
				val setterName = prop.setterName.function
				val setter = MethodDeclaration.newAbstract(setterName, reprType,
					description = prop.description,
					returnDescription = s"Copy of this factory with the specified ${ prop.name.doc }",
					isLowMergePriority = true)(
					Parameter(prop.setterParamName.prop, prop.dataType,
						description = s"New ${ prop.name.doc } to use. \n${prop.description}"))
				// Case: Mapping is enabled => Writes both the setter and the mapper
				if (prop.mappingEnabled) {
					val mapper = MethodDeclaration(("map" +: prop.name).function, isLowMergePriority = true)(
						Parameter("f", prop.dataType.fromParameters(prop.dataType)))(
						s"$setterName(f(${prop.name.function}))")
					Pair(setter, mapper)
				}
				// Case: Mapping is disabled => Writes the setter only
				else
					Some(setter)
			}.toSet ++ referencedMethods,
			description = s"Common trait for ${factory.componentName} factories and settings",
			author = factory.author,
			since = DeclarationDate.versionedToday
		)
	}
	
	private def settingsCompanion(name: String) =
		ObjectDeclaration(name, properties = Vector(ImmutableValue("default")("apply()")))
	
	// Declares the settings case class
	private def settings(factory: ComponentFactory, name: String, settingsLikeType: ScalaType)
	                    (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		val settingsType = ScalaType.basic(name)
		ClassDeclaration(
			name = name,
			// Defines all properties in the constructor
			constructionParams = factory.allProperties.map { prop =>
				Parameter(prop.name.prop, prop.dataType, prop.defaultValue, description = prop.description)
			},
			extensions = Vector(settingsLikeType(settingsType)),
			// Specifies a setter for each property
			methods = factory.allProperties.map { prop =>
				val paramName = prop.setterParamName.prop
				MethodDeclaration(prop.setterName.function, isOverridden = true, isLowMergePriority = true)(
					Parameter(paramName, prop.dataType))(
					s"copy(${prop.name.function} = $paramName)")
			}.toSet,
			description = s"Combined settings used when constructing ${factory.componentName.pluralDoc}",
			author = factory.author,
			since = DeclarationDate.versionedToday,
			isCaseClass = true
		)
	}
	
	// Creates a wrapper trait for the settings
	private def settingsWrapper(factory: ComponentFactory, settingsLikeType: ScalaType, settingsType: ScalaType)
	                           (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		TraitDeclaration(
			name = (factory.componentName + "SettingsWrapper").className,
			genericTypes = Vector(repr),
			extensions = Vector(settingsLikeType(reprType)),
			// Defines an abstract settings property
			properties = PropertyDeclaration
				.newAbstract("settings", settingsType, description = "Settings wrapped by this instance",
					isProtected = true) +:
				// Also defines all property getters
				factory.allProperties.map { prop =>
					val propName = prop.name.prop
					ComputedProperty(propName, visibility = Protected, isOverridden = true)(s"settings.$propName")
				},
			// Defines setters for each property
			methods = factory.allProperties.map { prop =>
				val setterName = prop.setterName.function
				val paramName = prop.setterParamName.prop
				MethodDeclaration(setterName, isOverridden = true, isLowMergePriority = true)(
					Parameter(paramName, prop.dataType))(s"mapSettings { _.$setterName($paramName) }")
			}.toSet +
				// Also defines abstract settings setter function
				MethodDeclaration.newAbstract("withSettings", reprType,
					returnDescription = "Copy of this factory with the specified settings")(
					Parameter("settings", settingsType)) +
				// Defines a mapper function for modifying settings
				MethodDeclaration("mapSettings")(
					Parameter("f", settingsType.fromParameters(settingsType)))(
					"withSettings(f(settings))"),
			description = s"Common trait for factories that wrap a ${factory.componentName} settings instance",
			author = factory.author,
			since = DeclarationDate.versionedToday
		)
	}
	
	// Creates the common component factory trait
	private def factoryLike(factory: ComponentFactory, settingsWrapperType: ScalaType)
	                       (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		TraitDeclaration(
			name = (factory.componentName + "FactoryLike").className,
			genericTypes = Vector(repr),
			extensions = Vector(settingsWrapperType(reprType)),
			// Defines the abstract parentHierarchy property
			properties = Vector(
				PropertyDeclaration.newAbstract("parentHierarchy", componentHierarchy,
					description = s"The component hierarchy, to which created ${
						factory.componentName.pluralDoc} will be attached",
					isProtected = true)
			),
			// Defines an apply method stub without implementation
			methods = Set(
				MethodDeclaration("apply", description = s"Creates a new ${factory.componentName}",
					returnDescription = s"A new ${factory.componentName}", isLowMergePriority = true)()("???")
			),
			description = s"Common trait for factories that are used for constructing ${factory.componentName.pluralDoc}",
			author = factory.author,
			since = DeclarationDate.versionedToday
		)
	}
	
	// Creates the non-contextual factory variant
	private def nonContextualFactory(factory: ComponentFactory, settingsType: ScalaType, factoryLikeType: ScalaType,
	                                 contextual: Option[(ScalaType, ContextType)] = None)
	                                (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		val name = (factory.componentName + "Factory").className
		val factoryType = ScalaType.basic(name)
		// Creates the extension and function for contextual version -accessing, if appropriate
		val contextualParentAndMethod = contextual.map { case (contextual, context) =>
			// If variable (pointer) context type is used, extends a different trait
			val (fromContextF, contextParamType) = {
				if (factory.useVariableContext)
					fromVariableContextFactory -> (flow.changing(context.reference): ScalaType)
				else
					fromContextFactory -> (context.reference: ScalaType)
			}
			val withContextMethod = MethodDeclaration("withContext", isOverridden = true)(
				Parameter("context", contextParamType))(
				s"$contextual(parentHierarchy, context, settings)")
			fromContextF(context.reference, contextual) -> withContextMethod
		}
		ClassDeclaration(
			name = name,
			// Contains the standard (implemented) properties + non-context -specific properties
			constructionParams = Vector(
				Parameter("parentHierarchy", componentHierarchy),
				Parameter("settings", settingsType, s"$settingsType.default")
			) ++ factory.nonContextualProperties.map { prop =>
				Parameter(prop.name.prop, prop.dataType, prop.defaultValue, description = prop.description)
			},
			extensions = factoryLikeType(factoryType) +:
				// Enables context-appending, if specified
				contextualParentAndMethod.map[Extension] { _._1 }.toVector,
			methods = Set(
				// Settings setter -function
				MethodDeclaration("withSettings", isOverridden = true)(
					Parameter("settings", settingsType))("copy(settings = settings)")) ++
				// Context append -function (optional)
				contextualParentAndMethod.map { _._2 } ++
				factory.nonContextualProperties.map { prop =>
					val paramName = prop.setterParamName.prop
					MethodDeclaration(prop.setterName.function,
						returnDescription = s"Copy of this factory with the specified ${prop.name}")(
						Parameter(paramName, prop.dataType, description = prop.description))(
						s"copy(${prop.name.prop} = $paramName)")
				},
			description = s"Factory class that is used for constructing ${
				factory.componentName.pluralDoc} without using contextual information",
			author = factory.author,
			since = DeclarationDate.versionedToday,
			isCaseClass = true
		)
	}
	
	// Creates the contextual factory variant
	// TODO: If non-contextual version is not used, skip the parent factory trait and only write this variant
	private def contextualFactory(factory: ComponentFactory, name: String, context: ContextType, settingsType: ScalaType,
	                              factoryLikeType: ScalaType)
	                             (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		val factoryType = ScalaType.basic(name)
		// If variable contexts are used, extends a different factory class,
		// different constructor parameter and different withContext -function
		val (contextualParent, contextParameter, withContextMethod) = {
			if (factory.useVariableContext) {
				val parentTrait = variableContextualFactory(context.reference, factoryType)
				val parameter = Parameter("contextPointer", flow.changing(context.reference))
				val withContextMethod = MethodDeclaration("withContextPointer", isOverridden = true)(parameter)(
					"copy(contextPointer = contextPointer)")
				(parentTrait, parameter, withContextMethod)
			}
			else {
				val parentTrait = context.factory(factoryType)
				val parameter = Parameter("context", context.reference)
				val withContextMethod = MethodDeclaration("withContext", isOverridden = true)(parameter)(
					"copy(context = context)")
				(parentTrait, parameter, withContextMethod)
			}
		}
		ClassDeclaration(
			name = name,
			// Contains the default parameters (parentHierarchy, context, settings),
			// plus possible custom properties
			constructionParams = Vector(
				Parameter("parentHierarchy", componentHierarchy),
				contextParameter,
				Parameter("settings", settingsType, s"$settingsType.default")
			) ++ factory.contextualProperties.map { prop =>
				Parameter(prop.name.prop, prop.dataType, prop.defaultValue, description = prop.description)
			},
			// TODO: Add support for contextual ReachFactoryTrait variants
			extensions = Vector(factoryLikeType(factoryType), contextualParent),
			// Implements the required setters
			methods = Set(
				withContextMethod,
				MethodDeclaration("withSettings", visibility = Protected, isOverridden = true)(
					Parameter("settings", settingsType))("copy(settings = settings)")
			) ++
				// Also contains possible custom property setters
				factory.contextualProperties.map { prop =>
					val paramName = prop.setterParamName.prop
					MethodDeclaration(prop.setterName.function,
						returnDescription = s"Copy of this factory with the specified ${prop.name}")(
						Parameter(paramName, prop.dataType, description = prop.description))(
						s"copy(${prop.name.prop} = $paramName)")
				},
			description = s"Factory class used for constructing ${
				factory.componentName.pluralDoc} using contextual component creation information",
			author = factory.author,
			since = DeclarationDate.versionedToday,
			isCaseClass = true
		)
	}
	
	// Creates the outside-hierarchy setup class
	// TODO: Add withContext variant that uses pointers (if applicable)
	private def setupFactory(factory: ComponentFactory, settingsType: ScalaType, settingsWrapperType: ScalaType,
	                         nonContextualFactoryType: Option[ScalaType],
	                         contextualFactoryType: Option[(ScalaType, ContextType)])
	                        (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		val name = (factory.componentName + "Setup").className
		val setupType = ScalaType.basic(name)
		
		// If non-contextual factory version is disabled, specifies different extension and methods
		val hierarchyParam = Parameter("hierarchy", componentHierarchy)
		val nonContextualParentAndMethod = nonContextualFactoryType.map { nonContextual =>
			cff(nonContextual) -> MethodDeclaration("apply", isOverridden = true)(
				hierarchyParam)(s"$nonContextual(hierarchy, settings)")
		}
		val contextualParentAndMethod = contextualFactoryType.map { case (contextual, context) =>
			// If the accepted context is variable, this class still accepts a static context
			val (methodImplementation, methodReferences) = {
				if (factory.useVariableContext)
					s"$contextual(hierarchy, Fixed(context), settings)" -> Set(flow.fixed)
				else
					s"$contextual(hierarchy, context, settings)" -> Set[Reference]()
			}
			ccff(context.reference, contextual) -> MethodDeclaration("withContext", methodReferences, isOverridden = true)(
				Vector(hierarchyParam, Parameter("context", context.reference)))(
				methodImplementation)
		}
		
		ClassDeclaration(
			name = (factory.componentName + "Setup").className,
			constructionParams = Vector(Parameter("settings", settingsType, s"$settingsType.default")),
			extensions = Vector[Extension](settingsWrapperType(setupType)) ++
				nonContextualParentAndMethod.map[Extension] { _._1 } ++
				contextualParentAndMethod.map[Extension] { _._1 },
			// Implements the required methods
			methods = Set(MethodDeclaration("withSettings", isOverridden = true)(
				Parameter("settings", settingsType))("copy(settings = settings)")) ++
				nonContextualParentAndMethod.map { _._2 } ++ contextualParentAndMethod.map { _._2 },
			description = s"Used for defining ${
				factory.componentName} creation settings outside of the component building process",
			author = factory.author,
			since = DeclarationDate.versionedToday,
			isCaseClass = true
		)
	}
	
	// Creates the component companion object that serves as the root access point
	private def companion(factory: ComponentFactory, settingsType: ScalaType, setupType: ScalaType)
	                     (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		ObjectDeclaration(
			name = factory.componentName.className,
			extensions = Vector(Extension(setupType, Vector(Vector()))),
			// Provides a public apply function for settings-assignment
			methods = Set(MethodDeclaration("apply")(Parameter("settings", settingsType))("withSettings(settings)")),
			author = factory.author,
			since = DeclarationDate.versionedToday
		)
	}
	
	private def referencedPropFunctionsFrom(referenceProperty: Property, target: ComponentFactory, prefix: Name)
	                                       (implicit naming: NamingRules): (Vector[PropertyDeclaration], Vector[MethodDeclaration]) =
	{
		val base = referenceProperty.name.prop
		target.allProperties.splitFlatMap { prop =>
			// Generates a getter
			val propName = if (referenceProperty.prefixDerivedProperties) prefix +: prop.name else prop.name
			val directGet = ComputedProperty(propName.prop, visibility = Protected,
				description = s"${prop.name} from the wrapped ${target.componentName} settings")(
				s"$base.${prop.name.prop}")
			// Generates a setter
			val paramName = prop.setterParamName.prop
			val directSet = MethodDeclaration(("with" +: propName).function,
				returnDescription = s"Copy of this factory with the specified $propName")(
				Parameter(paramName, prop.dataType, description = prop.description))(
				s"${referenceProperty.setterName.function}($base.${prop.setterName.function}($paramName))")
			// Generates a mapper, if enabled
			val mapper = {
				if (prop.mappingEnabled)
					Some(MethodDeclaration(("map" +: propName).function)(
						Parameter("f", prop.dataType.fromParameters(prop.dataType)))(
						s"${directSet.name}(f(${directGet.name}))"))
				else
					None
			}
			// Recursively generates more properties if the reference goes deeper
			val (moreProps, moreMethods) = prop.reference match {
				case Some((deeperTarget, nextPrefix)) =>
					val appliedPrefix = if (referenceProperty.prefixDerivedProperties) prefix + nextPrefix else nextPrefix
					referencedPropFunctionsFrom(prop, deeperTarget, appliedPrefix)
				case None => Vector() -> Vector()
			}
			(directGet +: moreProps) -> ((directSet +: mapper.toVector) ++ moreMethods)
		}
	}
}
