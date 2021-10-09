package utopia.vault.coder.controller.writer

import utopia.flow.datastructure.immutable.Pair
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.Visibility.Private
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.scala.{Parameter, Reference, ScalaType}
import utopia.vault.coder.model.scala.declaration.{File, MethodDeclaration, ObjectDeclaration}

import scala.io.Codec

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
	  * @param codec Codec to use when writing the file (implicit)
	  * @param setup Target project -specific settings (implicit)
	  * @return Reference to the written object. Failure if writing failed.
	  */
	def apply(classes: Iterable[Class])(implicit codec: Codec, setup: ProjectSetup) =
	{
		val objectName = setup.projectPackage.parts.last.capitalize + "Tables"
		// If one of the classes uses descriptions, considers Citadel to be used
		// and bases the apply method on that knowledge
		// Otherwise leaves the implementation to the user
		val (applyImplementation, applyReferences) =
		{
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
					c.descriptionLinkClass match
					{
						case Some(descriptionLinkClass) =>
							Pair(tablePropertyFrom(c), tablePropertyFrom(descriptionLinkClass))
						case None => Vector(tablePropertyFrom(c))
					}
				},
				// Defines a private apply method but leaves the implementation open
				methods = Set(MethodDeclaration("apply", applyReferences, Private, Some(Reference.table))(
					Parameter("tableName", ScalaType.string))(applyImplementation.head, applyImplementation.tail: _*)),
				description = "Used for accessing the database tables introduced in this project",
				author = classes.map { _.author }.toSet.filter { _.nonEmpty }.mkString(", ")
			)
		).write()
	}
	
	private def tablePropertyFrom(c: Class) =
	{
		val baseDescription = s"Table that contains ${c.name.plural}"
		val completeDescription = if (c.description.isEmpty) baseDescription else
			s"$baseDescription (${c.description})"
		ComputedProperty(c.name.singular.uncapitalize, description = completeDescription)(
			s"apply(${c.tableName.quoted})")
	}
}
