package utopia.vault.coder.controller.writer.documentation

import utopia.flow.time.Today
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Name, NamingRules, ProjectData}
import utopia.vault.coder.model.datatype.PropertyType.{ClassReference, EnumValue}
import utopia.vault.coder.model.enumeration.CombinationType.{Combined, MultiCombined, PossiblyCombined}
import utopia.vault.coder.model.enumeration.NameContext.Header
import utopia.vault.coder.model.enumeration.NamingConvention.{CamelCase, Hyphenated}

import java.nio.file.Path
import scala.io.Codec

/**
  * Writes .md documentation
  * @author Mikko Hilpinen
  * @since 20.8.2022, v1.7
  */
object DocumentationWriter
{
	/**
	  * Writes class and enumeration structure as an .md documentation
	  * @param data Project data to write
	  * @param targetPath Path to the .md documentation file
	  * @param codec Implicit codec to use when writing
	  * @param naming Implicit naming rules
	  * @return Success or failure
	  */
	def apply(data: ProjectData, targetPath: Path)
	         (implicit codec: Codec, naming: NamingRules) =
	{
		targetPath.writeUsing { writer =>
			// Writes the project header
			writer.println(s"# ${ data.projectName.header }")
			data.version.foreach { v => writer.println(s"Version: **$v**  ") }
			writer.println(s"Updated: $Today")
			
			// Orders the enumerations
			val orderedEnums = data.enumerations.map { e => e.name.inContext(Header) -> e }.sortBy { _._1 }
			// Groups the classes by package
			val orderedClasses = data.classes.groupBy { _.packageName }
				.map { case (packageName, classes) =>
					val packageHeader = Name.interpret(packageName, CamelCase.lower).inContext(Header)
					val orderedClasses = classes.map { c => c.name.inContext(Header) -> c }.sortBy { _._1 }
					packageHeader -> orderedClasses
				}
				.toVector.sortBy { _._1 }
			
			// Tracks class references
			val classByTableName = data.classes.map { c => c.tableName -> c }.toMap
			val referencesPerTarget = data.classes.flatMap { c =>
				c.properties.flatMap { prop =>
					prop.dataType match {
						case ClassReference(tableName, _, _) =>
							classByTableName.get(tableName.table).map { target => (target, c, prop) }
						case _ => None
					}
				}
			}.groupMap { _._1 } { case (_, origin, originProp) => origin -> originProp }
			val classesPerEnum = data.classes.flatMap { c =>
				c.properties.flatMap { prop =>
					prop.dataType match {
						case EnumValue(enum) => Some(enum -> c)
						case _ => None
					}
				}
			}.asMultiMap
			val combinationsByParent = data.combinations.map { c => c.parentClass -> c }.asMultiMap
			
			// Writes the table of contents
			writer.println("\n## Table of Contents")
			if (orderedEnums.nonEmpty) {
				writer.println("- [Enumerations](#enumerations)")
				orderedEnums.foreach { case (name, _) => writer.println(s"  - ${ link(name) }") }
			}
			if (orderedClasses.nonEmpty) {
				writer.println("- [Packages & Classes](#packages-and-classes)")
				orderedClasses.foreach { case (packageName, classes) =>
					writer.println(s"  - ${ link(packageName) }")
					classes.foreach { case (className, _) => writer.println(s"    - ${ link(className) }") }
				}
			}
			
			// Writes enumeration descriptions
			writer.println(s"\n## Enumerations\nBelow are listed all enumerations introduced in ${
				data.projectName.header }, in alphabetical order  ")
			orderedEnums.foreach { case (name, enum) =>
				writer.println(s"\n### ${ name.header }")
				enum.description.notEmpty.foreach { doc => writer.println(doc) }
				writer.println(s"\nKey: `${ enum.idPropName.prop }: ${ enum.idType.toScala }`  ")
				enum.defaultValue.foreach { default =>
					writer.println(s"Default Value: **${ default.name.header }**")
				}
				// Lists each enum value
				writer.println("\n**Values:**")
				enum.values.foreach { v =>
					writer.println(s"- **${ v.name.header }** (${ v.id })${ v.description.mapIfNotEmpty { " - " + _ } }")
				}
				// Lists classes that use this enumeration
				classesPerEnum.get(enum).foreach { classes =>
					writer.println(s"\nUtilized by the following ${classes.size} classes:")
					classes.sortBy { _.name }.foreach { c => writer.println(s"- ${ link(c.name) }") }
				}
			}
			
			// Writes packages and classes
			writer.println("\n## Packages and Classes")
			writer.println(s"Below are listed all classes introduced in ${ data.projectName.header }, grouped by package and in alphabetical order.  ")
			writer.println(s"There are a total number of ${ orderedClasses.size } packages and ${ orderedClasses.map { _._2.size }.sum } classes")
			orderedClasses.foreach { case (packageName, classes) =>
				writer.println(s"\n### ${ packageName.header }")
				writer.println(s"This package contains the following ${ classes.size } classes: ${
					classes.map { case (c, _) => link(c) }.mkString(", ") }")
				
				classes.foreach { case (className, c) =>
					writer.println(s"\n#### ${ className.header }")
					c.description.notEmpty.foreach(writer.println)
					
					writer.println("\n##### Details")
					c.customTableName.foreach { t => s"- Appears in the database as: `$t`" }
					if (c.idName !~== Class.defaultIdName)
						writer.println(s"- Uses a **custom id: ${ c.idName.header }**")
					if (c.useLongId)
						writer.println(s"- Uses very **large ids**")
					if (c.isDescribed)
						writer.println(s"- Utilizes **localized descriptions**")
					combinationsByParent.get(c).foreach { combos =>
						combos.sortBy { _.childClass.name }.foreach { combo =>
							val start = combo.combinationType match {
								case Combined => s"Combines with ${ link(combo.childClass.name) }"
								case PossiblyCombined => s"May combine with ${ link(combo.childClass.name) }"
								case MultiCombined =>
									val possible = if (combo.isAlwaysLinked) "" else " possibly"
									s"Combines with$possible multiple [${
										combo.childClass.name.pluralInContext(Header) }](#${
										combo.childClass.name.singularIn(Hyphenated) })"
							}
							writer.println(s"- $start, creating a **${ combo.name.header }**")
						}
					}
					val isChronological = c.recordsIndexedCreationTime
					val deprecates = c.isDeprecatable
					if (isChronological && deprecates)
						writer.println("- Fully **versioned**")
					else if (deprecates)
						writer.println("- Preserves **history**")
					else if (isChronological)
						writer.println("- **Chronologically** indexed")
					val comboIndices = c.comboIndexColumnNames.map { list => list.map { c => s"`$c`" }.mkString(" => ") }
					if (comboIndices.nonEmpty) {
						if (comboIndices.size > 1) {
							writer.println(s"- Uses **${ comboIndices.size } combo indices**:")
							comboIndices.foreach { idx => writer.println(s"  - $idx") }
						}
						else
							writer.println(s"- Uses a **combo index**: ${ comboIndices.head }")
					}
					val indices = c.dbProperties.filter { _.isIndexed }.map { p => s"`${p.columnName}`" }
					if (indices.nonEmpty) {
						if (indices.size > 1)
							writer.println(s"- Uses ${ indices.size } database **indices**: ${ indices.mkString(", ") }")
						else
							writer.println(s"- Uses **index**: ${ indices.head }")
					}
					
					// Writes class properties
					writer.println("\n##### Properties")
					writer.println(s"${ className.header } contains the following ${ c.properties.size } properties:")
					c.properties.foreach { prop =>
						writer.println(s"- **${ prop.name.header }** - `${ prop.dataType.toScala }`${
							prop.customDefaultValue.text.mapIfNotEmpty { d => s", `$d` by default" } }${
							prop.description.mapIfNotEmpty { " - " + _ } }")
						prop.referencedTableName.foreach { tableName =>
							val actualTableName = tableName.table
							classByTableName.get(actualTableName) match {
								case Some(target) => writer.println(s"  - Refers to ${ link(target.name) }")
								case None => writer.println(s"  - Refers to $actualTableName from another module")
							}
						}
					}
					
					// Writes references
					referencesPerTarget.get(c).foreach { refs =>
						writer.println("\n##### Referenced from")
						refs.sortBy { _._1.name }.foreach { case (origin, prop) =>
							writer.println(s"- ${ link(origin.name) }.`${ prop.name.prop }`")
						}
					}
				}
			}
		}
	}
	
	private def link(name: Name)(implicit naming: NamingRules) = s"[${ name.header }](#${ name.singularIn(Hyphenated) })"
}
