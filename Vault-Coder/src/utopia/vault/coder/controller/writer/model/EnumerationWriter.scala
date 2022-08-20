package utopia.vault.coder.controller.writer.model

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Enum, Name, NamingRules, ProjectSetup}
import utopia.vault.coder.model.enumeration.NamingConvention.CamelCase
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.datatype.{Extension, Reference, ScalaType}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue}
import utopia.vault.coder.model.scala.declaration.{File, MethodDeclaration, ObjectDeclaration, PropertyDeclaration, TraitDeclaration}
import utopia.vault.coder.model.scala.{DeclarationDate, Parameter}

import scala.io.Codec

/**
  * Used for writing enumeration files
  * @author Mikko Hilpinen
  * @since 25.9.2021, v1.1
  */
object EnumerationWriter
{
	lazy val findPrefix = Name("find", CamelCase.lower)
	lazy val forPrefix = Name("for", CamelCase.lower)
	
	/**
	  * @param e Enumeration
	  * @param naming Implicit naming rules
	  * @return Name of the 'findForId' -function
	  */
	def findForIdName(e: Enum)(implicit naming: NamingRules) = (findPrefix + forPrefix + e.idPropName).function
	/**
	  * @param e Enumeration
	  * @param naming Implicit naming rules
	  * @return Name of the 'forId' -function
	  */
	def forIdName(e: Enum)(implicit naming: NamingRules) = (forPrefix + e.idPropName).function
	
	/**
	  * Writes an enumeration as a scala file
	  * @param e  Enumeration to write
	  * @param setup Project setup to use (implicit)
	  * @param codec Codec to use (implicit)
	  * @return Enum reference on success. Failure if writing failed.
	  */
	def apply(e: Enum)(implicit setup: ProjectSetup, codec: Codec, naming: NamingRules) =
	{
		val enumName = e.name.enumName
		val idPropName = e.idPropName.prop
		val _findForIdName = findForIdName(e)
		val _forIdName = forIdName(e)
		// Enumeration doesn't need to be imported in its own file
		val enumDataType = ScalaType.basic(enumName)
		
		val defaultProp = e.defaultValue.map { v =>
			ComputedProperty("default", description = s"The default ${ e.name.doc } (i.e. ${ v.name.doc })")(
				v.name.enumValue)
		}
		// forId implementation differs when the enumeration has a default value
		val (forIdEndCode, forIdDescriptionPostfix) = e.defaultValue match {
			case Some(default) => CodePiece(".getOrElse(default)") -> s", or the default ${e.name} (${default.name})"
			case None =>
				CodePiece(s".toTry { new NoSuchElementException(s${
					s"No value of ${ e.name.doc } matches ${ e.idPropName.doc } '${ "$" + idPropName }'".quoted
				}) }", Set(Reference.collectionExtensions, Reference.noSuchElementException)) ->
					". Failure if no matching value was found."
		}
		// NB: Current implementation doesn't really work for multi-column id types
		val fromValueCode = e.idType.fromValueCode(Vector("value")).mapText { id =>
			if (e.idType.yieldsTryFromValue) {
				if (e.hasDefault) s"$id.map(${_forIdName})" else s"$id.flatMap(${_forIdName})"
			}
			else
				s"${_forIdName}($id)"
		}
		val enumValueToValueCode = e.idType.toValueCode(idPropName)
		
		File(e.packagePath,
			// Writes the enumeration trait first
			TraitDeclaration(enumName,
				extensions = Vector(Reference.valueConvertible),
				// Each value contains an id so that it can be referred from the database
				properties = Vector(
					PropertyDeclaration.newAbstract(idPropName, e.idType.toScala,
						description = s"${e.idPropName} used to represent this ${e.name} in database and json"),
					ComputedProperty("toValue", enumValueToValueCode.references, isOverridden = true)(
						enumValueToValueCode.text)
				),
				description = e.description.nonEmptyOrElse(s"Common trait for all ${ e.name.doc } values"),
				author = e.author, since = DeclarationDate.versionedToday, isSealed = true
			),
			// Enumeration values are nested within a companion object
			ObjectDeclaration(enumName,
				// Contains the .values -property
				properties = Vector(
					ImmutableValue("values", explicitOutputType = Some(ScalaType.vector(enumDataType)),
						description = s"All available ${e.name} values")(
						s"Vector(${ e.values.map { v => v.name.enumValue }.mkString(", ") })")
				) ++ defaultProp,
				// Contains an id to enum value -function (one with Try, another with Option)
				methods = Set(
					MethodDeclaration(_findForIdName,
						returnDescription = s"${ e.name.doc } matching the specified ${ e.idPropName.doc }. None if the ${
							e.idPropName.doc } didn't match any ${ e.name.doc }", isLowMergePriority = true)(
						Parameter(idPropName, e.idType.toScala, description = s"${ e.idPropName.doc } representing a ${ e.name.doc }"))(
						s"values.find { _.$idPropName == $idPropName }"),
					MethodDeclaration(_forIdName,
						codeReferences = forIdEndCode.references,
						returnDescription = s"${ e.name.doc } matching that ${ e.idPropName.doc }$forIdDescriptionPostfix")(
						Parameter(idPropName, e.idType.toScala, description = s"${ e.idPropName.doc } matching a ${ e.name.doc }"))(
						s"${_findForIdName}($idPropName)${forIdEndCode.text}"),
					MethodDeclaration("fromValue", fromValueCode.references,
						returnDescription = s"${ e.name.doc } matching the specified value, when the value is interpreted as an ${
							e.name.doc } ${ e.idPropName.doc }$forIdDescriptionPostfix")(
						Parameter("value", Reference.value,
							description = s"A value representing an ${ e.name.doc } ${ e.idPropName.doc }"))(
						fromValueCode.text)
				),
				// Contains an object for each value
				nested = e.values.map { value =>
					ObjectDeclaration(value.name.enumValue, Vector(Extension(enumDataType)),
						// The objects don't contain other properties except for 'id'
						properties = Vector(
							ImmutableValue(idPropName, value.id.references, isOverridden = true)(value.id.text)),
						description = value.description, isCaseObject = true
					)
				}.toSet
			)
		).write()
	}
}
