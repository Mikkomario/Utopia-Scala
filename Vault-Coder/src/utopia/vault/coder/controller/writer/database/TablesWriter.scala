package utopia.vault.coder.controller.writer.database

import utopia.coder.model.data.NamingRules
import utopia.flow.collection.immutable.Pair
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, VaultProjectSetup}
import utopia.coder.model.scala.Visibility.Private
import utopia.coder.model.scala.datatype.{Reference, ScalaType}
import utopia.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, LazyValue}
import utopia.coder.model.scala.declaration.{File, MethodDeclaration, ObjectDeclaration}
import utopia.coder.model.scala.{DeclarationDate, Parameter}

import scala.io.Codec
import scala.util.Success

/**
  * Used for writing the tables -file
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object TablesWriter
{
	/**
	  * Writes the table reference file
	  * @param classes Classes introduced in this project
	  * @param codec   Codec to use when writing the file (implicit)
	  * @param setup   Target project -specific settings (implicit)
	  * @return Reference to the written object. Failure if writing failed.
	  */
	def apply(classes: Iterable[Class])(implicit codec: Codec, setup: VaultProjectSetup, naming: NamingRules) =
	{
		val objectName = (setup.dbModuleName + "Tables").objectName
		// If there are no classes to write, omits this document (e.g. when only writing enumerations or something)
		if (classes.isEmpty)
			Success(Reference(setup.databasePackage, objectName))
		else {
			// If one of the classes uses descriptions, considers Citadel to be used
			// and bases the apply method on that knowledge
			// Otherwise leaves the implementation to the user
			val (applyImplementation, applyReferences) = {
				if (classes.exists { _.isDescribed })
					Vector("Tables(tableName)") -> Set[Reference](Reference.citadelTables)
				else
					Vector("// TODO: Refer to a tables instance of your choice",
						"// If you're using the Citadel module, import utopia.citadel.database.Tables",
						"// Tables(tableName)",
						"???") -> Set[Reference]()
			}
			File(setup.databasePackage,
				ObjectDeclaration(objectName,
					// Contains a computed property for each class / table
					properties = classes.toVector.sortBy { _.name }.flatMap { c =>
						c.descriptionLinkClass match {
							case Some(descriptionLinkClass) =>
								Pair(tablePropertyFrom(c), descriptionLinkTablePropertyFrom(descriptionLinkClass))
							case None => Vector(tablePropertyFrom(c))
						}
					},
					// Defines a private apply method but leaves the implementation open
					methods = Set(MethodDeclaration("apply", applyReferences, visibility = Private,
						explicitOutputType = Some(Reference.table), isLowMergePriority = true)(
						Parameter("tableName", ScalaType.string))(
						applyImplementation.head, applyImplementation.tail: _*)),
					description = "Used for accessing the database tables introduced in this project",
					author = classes.map { _.author }.toSet.filter { _.nonEmpty }.mkString(", "),
					since = DeclarationDate.versionedToday
				)
			).write()
		}
	}
	
	private def tablePropertyFrom(c: Class)(implicit naming: NamingRules) =
		ComputedProperty(c.name.prop, description = tablePropertyDescriptionFrom(c))(
			s"apply(${ c.tableName.quoted })")
	private def descriptionLinkTablePropertyFrom(c: Class)(implicit naming: NamingRules) =
	{
		val linkProp = c.properties.head
		LazyValue(c.name.prop, Set(Reference.descriptionLinkTable),
			description = tablePropertyDescriptionFrom(c))(
			s"DescriptionLinkTable(apply(${c.tableName.quoted}), ${linkProp.name.prop.quoted})")
	}
	
	private def tablePropertyDescriptionFrom(c: Class)(implicit naming: NamingRules) =
	{
		val baseDescription = s"Table that contains ${ c.name.pluralDoc }"
		if (c.description.isEmpty)
			baseDescription
		else
			s"$baseDescription (${ c.description })"
	}
}
