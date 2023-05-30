package utopia.reach.coder.controller.reader

import utopia.bunnymunch.jawn.JsonBunny
import utopia.coder.model.data.{Name, NamingRules, ProjectSetup}
import utopia.coder.model.enumeration.NameContext.{ClassName, ClassPropName}
import utopia.coder.model.enumeration.NamingConvention
import utopia.coder.model.enumeration.NamingConvention.CamelCase
import utopia.coder.model.scala.Package
import utopia.coder.model.scala.datatype.Reference
import utopia.flow.collection.mutable.builder.{CompoundingMapBuilder, CompoundingVectorBuilder}
import utopia.flow.collection.template.MapAccess
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.DataType.{ModelType, VectorType}
import utopia.flow.util.Version
import utopia.flow.util.StringExtensions._
import utopia.reach.coder.model.data.{ComponentFactory, Property}
import utopia.reach.coder.model.enumeration.{ContextType, ReachFactoryTrait}

import java.nio.file.Path

/**
  * Used for reading component factory data from json
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.0
  */
object ComponentFactoryReader
{
	def apply(path: Path) = JsonBunny(path).map { rootV =>
		val root = rootV.getModel
		// Parses standard project information
		val projectName = root("project").stringOr("Project")
		val version = root("version").string.flatMap { Version.findFrom }
		val author = root("author").getString
		val basePackage = Package(root("package").getString)
		// Parses package aliases and reference aliases
		val packageAliases = packageAliasesFrom(root("package_aliases", "packages").getModel)
		val referenceAliases = referenceAliasesFrom(root("reference_aliases", "references", "aliases").getModel,
			packageAliases)
		// Parses the component factories
		
		???
	}
	
	private def packageAliasesFrom(aliases: Model) = {
		val builder = new CompoundingMapBuilder[String, Package]()
		// Each property represents a single package alias
		aliases.properties.foreach { prop =>
			val name = prop.name
			val pck = prop.value.getString
			// Case: / is used to refer to another package alias
			if (pck.contains('/')) {
				val (referred, afterRef) = pck.splitAtFirst("/")
				val fullPackage = builder.get(referred) match {
					// Case: Referred alias could be discerned from previously specified aliases
					case Some(parent) => parent/afterRef
					// Case: Referred alias couldn't be discerned => Prints a warning
					case None =>
						println(s"Warning: Reference '$referred' couldn't be determined for package alias '$name'")
						Package(s"$referred.$afterRef")
				}
				builder += (name -> fullPackage)
			}
			// Case: This alias doesn't make references
			else
				builder += (name -> Package(pck))
		}
		builder.result()
	}
	
	private def referenceAliasesFrom(aliases: Model, packageAliases: Map[String, Package]) = {
		// Each property represents a reference alias
		aliases.properties.map { prop =>
			val name = prop.name
			val ref = prop.value.getString
			val reference = {
				// Case: The reference refers to a package alias
				if (ref.contains('/')) {
					val (referred, afterRef) = ref.splitAtFirst("/")
					packageAliases.get(referred) match {
						// Case: Package alias could be resolved
						case Some(parentPackage) => Reference(s"$parentPackage.$afterRef")
						// Case: Undefined package alias => Shows a warning
						case None =>
							println(s"Warning: Package reference '$referred' couldn't be determined for alias '$name'")
							Reference(s"$referred.$afterRef")
					}
				}
				// Case: Reference without a package reference
				else
					Reference(ref)
			}
			name -> reference
		}.toMap
	}
	
	private def parseComponentFactoriesFrom(packagesModel: Model, parentPackage: Package,
	                                        packageAliases: Map[String, Package],
	                                        referenceAliases: Map[String, Reference]): Vector[ComponentFactory] =
	{
		// Each property is expected to represent a package
		// Each property may either contain
		//      1) Another package object, or
		//      2) An array of component factory objects
		packagesModel.properties.flatMap { prop =>
			prop.value.castTo(ModelType, VectorType) match {
				case Left(packageModelV) =>
					parseComponentFactoriesFrom(packageModelV.getModel, parentPackage/prop.name, packageAliases,
						referenceAliases)
				case Right(factoryArrayV) =>
					factoryArrayV.getVector.map { v => ??? }
			}
		}
	}
	
	private def componentFactoryFrom(model: Model, parentPackage: Package, packageAliases: Map[String, Package],
	                                 referenceAliases: Map[String, Reference], projectAuthor: String)
	                                (implicit naming: NamingRules) =
	{
		ComponentFactory(
			pck = parentPackage,
			componentName = ClassName.from(model)
				.getOrElse(Name.interpret("UnnamedComponent", CamelCase.capitalized)),
			contextType = model("context").string.flatMap { input =>
				val result = ContextType(input)
				if (result.isEmpty)
					println(s"Warning: No context type matches '$input'")
				result
			},
			parentTraits = model("parents", "parent").getVector.flatMap { _.string.flatMap { input =>
				val result = ReachFactoryTrait(input)
				if (input.isEmpty)
					println(s"Warning: No Reach factory trait matches '$input'")
				result
			} },
			properties = ???,
			nonContextualProperties = ???,
			contextualProperties = ???,
			author = model("author").stringOr(projectAuthor),
			onlyContextual = !model("nonContextual").booleanOr(true)
		)
	}
	
	private def propertyFrom(model: Model, packageAliases: Map[String, Package],
	                         referenceAliases: Map[String, Reference])
	                        (factoryReferences: String => Option[ComponentFactory])
	                        (implicit naming: NamingRules) =
	{
		val description = model("description", "doc").getString
		model("reference", "ref", "factory", "settings").string match {
			case Some(reference) =>
				factoryReferences(reference) match {
					case Some(factory) =>
						val prefix = model("prefix").string match {
							case Some(prefix) =>
								model("prefix_plural").string match {
									case Some(plural) =>
										Name(prefix, plural, NamingConvention.of(prefix, naming(ClassPropName)))
									case None => Name.interpret(prefix, naming(ClassPropName))
								}
							case None => Name.empty
						}
						Property.referringTo(factory, prefix, description)
					case None =>
						println(s"Couldn't resolve component factory reference '$reference'")
						???
				}
			case None =>
				/*
				(name: Name, dataType: ScalaType, defaultValue: CodePiece = CodePiece.empty, description: String = "",
							mappingEnabled: Boolean = false)
				 */
				// TODO: Add support for aliases in code fromValue parsing
				Property.simple(ClassPropName.from(model).getOrElse { "unnamedProperty" }, ???, ???, description,
					model("mapping_enabled", "mapping", "map").getBoolean)
		}
	}
}
