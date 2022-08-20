package utopia.vault.coder.controller.writer.database

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.{BooleanType, DoubleType, FloatType, IntType, LongType}
import utopia.flow.time.Today
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.CombinedOrdering
import utopia.flow.util.FileExtensions._
import utopia.vault.coder.model.data.{Class, Instance, Name, NamingRules, ProjectSetup}
import utopia.vault.coder.model.enumeration.NamingConvention.{CamelCase, Text}

import java.io.PrintWriter
import java.nio.file.Path
import scala.io.Codec
import scala.util.Success

/**
  * Writes the initial sql inserts
  * @author Mikko Hilpinen
  * @since 18.2.2022, v1.5
  */
object InsertsWriter
{
	// ATTRIBUTES   --------------------------
	
	private implicit val codec: Codec = Codec.UTF8
	
	
	// OTHER    ------------------------------
	
	/**
	  * Writes initial database inserts SQL
	  * @param dbName Name of the targeted database (optional)
	  * @param instances Instances to write
	  * @param targetPath Path to which the data will be written
	  * @param setup Implicit project setup
	  * @param naming Implicit naming rules
	  * @return Success (containing target path) or failure
	  */
	def apply(dbName: Option[String], instances: Iterable[Instance], targetPath: Path)
	         (implicit setup: ProjectSetup, naming: NamingRules) =
	{
		if (instances.nonEmpty)
			targetPath.writeUsing { writer =>
				// Writes the header
				writer.println("-- ")
				writer.println(s"-- Initial database inserts for ${setup.dbModuleName}")
				setup.version.foreach { v => writer.println(s"-- Version: $v") }
				writer.println(s"-- Last generated: ${Today.toString}")
				writer.println("--")
				
				// Writes USE db statement (optional)
				dbName.foreach { dbName =>
					writer.println()
					writer.println(s"USE $dbName;")
				}
				
				// Groups the instances based on package and class
				instances.groupBy { _.parentClass }.groupBy { _._1.packageName }
					.toVector.sortBy { _._1 }
					.foreach { case (packageName, classes) =>
						// Writes the package header
						val packageHeader = Name.interpret(packageName, CamelCase.lower)
							.to(Text.allCapitalized).singular
						writer.println(s"\n--\t$packageHeader\t${"-" * 10}\n")
						
						// Writes one class' inserts at a time
						classes.toVector.sortBy { _._1.name.singular }.foreach { case (parentClass, instances) =>
							writeClassInstances(writer, parentClass, instances)
						}
					}
			}
		else
			Success(targetPath)
	}
	
	private def writeClassInstances(writer: PrintWriter, parentClass: Class, instances: Iterable[Instance])
	                               (implicit naming: NamingRules) =
	{
		val classDocName = if (instances.size > 1) parentClass.name.pluralDoc else parentClass.name.doc
		val tableName = parentClass.tableName
		writer.println(s"-- Inserts ${instances.size} $classDocName")
		// Instances with ids are written separate from instances without id
		val (instancesWithoutId, instancesWithId) = instances.divideBy { _.id.nonEmpty }
		if (instancesWithId.nonEmpty)
			writeInstanceInserts(writer, tableName, instancesWithId, Some(parentClass.idName.column))
		if (instancesWithoutId.nonEmpty)
			writeInstanceInserts(writer, tableName, instancesWithoutId)
		writer.println()
	}
	
	private def writeInstanceInserts(writer: PrintWriter, tableName: String, instances: Iterable[Instance],
	                                 idColumnName: Option[String] = None)
	                                (implicit naming: NamingRules) =
	{
		// Some inserts may need to be made separately because different properties are defined
		instances.groupBy { _.valueAssignments.keySet }.toVector.sortBy { _._1.size }
			.foreach { case (properties, instances) =>
				// Writes properties in alphabetical order
				val orderedProperties = properties.toVector.map { p => p.name.column -> p }.sortBy { _._1 }
				// Writes instances in order of id, or in order of alphabetical property values
				val orderedInstances = {
					if (idColumnName.isDefined)
						instances.toVector.sortBy { _.id.getLong }
					else {
						implicit val ordering: Ordering[Instance] = new CombinedOrdering[Instance](
							orderedProperties.map { case (_, prop) =>
								Ordering.by[Instance, String] { _.valueAssignments(prop).getString }
							})
						instances.toVector.sorted
					}
				}
				val lastInstance = orderedInstances.last
				
				writer.println(s"INSERT INTO `$tableName` (${
					(idColumnName.toVector ++ orderedProperties.map { _._1 })
						.map { colName => s"`$colName`" }.mkString(", ")}) VALUES ")
				val orderedInstancesWithSeparators = orderedInstances.dropRight(1).map { i => i -> ", " } :+
					(lastInstance -> ";")
				orderedInstancesWithSeparators.foreach { case (instance, separator) =>
					val propertySqlAssignments = orderedProperties
						.map { case (_, prop) => valueToSql(instance.valueAssignments(prop)) }
					val allSqlAssignments = {
						if (idColumnName.isDefined)
							valueToSql(instance.id) +: propertySqlAssignments
						else
							propertySqlAssignments
					}
					writer.println(s"\t(${ allSqlAssignments.mkString(", ") })$separator")
				}
			}
	}
	
	private def valueToSql(value: Value) = value.dataType match {
		// Numbers are written as they are
		case IntType => value.getInt.toString
		case LongType => value.getLong.toString
		case DoubleType => value.getDouble.toString
		case FloatType => value.getFloat.toString
		case BooleanType => if (value.getBoolean) "TRUE" else "FAlSE"
		// Other types are written as strings, wrapped in '' (or written as NULL)
		case _ =>
			value.string match {
				case Some(str) => s"'$str'"
				case None => "NULL"
			}
	}
}
