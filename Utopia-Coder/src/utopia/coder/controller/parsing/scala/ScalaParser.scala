package utopia.coder.controller.parsing.scala

import utopia.bunnymunch.jawn.JsonBunny
import utopia.coder.model.scala.Visibility.{Private, Protected, Public}
import utopia.coder.model.scala.code.{Code, CodeBuilder, CodeLine, CodePiece, ReadCodeBlock}
import utopia.coder.model.scala.datatype.ScalaTypeCategory.{CallByName, Standard}
import utopia.coder.model.scala.datatype.TypeVariance.Invariance
import utopia.coder.model.scala.datatype._
import utopia.coder.model.scala.declaration.DeclarationPrefix.{Implicit, Lazy, Override}
import utopia.coder.model.scala.declaration.FunctionDeclarationType.{FunctionD, ValueD, VariableD}
import utopia.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue, LazyValue, Variable}
import utopia.coder.model.scala.declaration._
import utopia.coder.model.scala.doc.{ScalaDoc, ScalaDocKeyword, ScalaDocPart}
import utopia.coder.model.scala.{Annotation, Package, Parameter, Parameters}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.builder.MultiMapBuilder
import utopia.flow.collection.mutable.iterator.PollingIterator
import utopia.flow.parse.string.{IterateLines, Regex}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.MutatingOnce

import java.nio.file.Path
import scala.collection.immutable.VectorBuilder

/**
  * Used for reading and interpreting scala files
  * @author Mikko Hilpinen
  * @since 29.10.2021, v1.3
  */
object ScalaParser
{
	private lazy val packageRegex = Regex("package ") + Regex.any
	private lazy val importRegex = Regex("import ") + Regex.any
	
	private lazy val annotationStartRegex = Regex.escape('@')
	private lazy val visibilityRegex = (Regex("protected ") || Regex("private ")).withinParenthesis
	private lazy val declarationPrefixRegex = DeclarationPrefix.values.map { p => Regex(s"${ p.keyword } ") }
		.reduceLeft { _ || _ }.withinParenthesis
	private lazy val declarationModifierRegex = (visibilityRegex || declarationPrefixRegex).withinParenthesis
	private lazy val declarationKeywordRegex = DeclarationType.values.map { d => Regex(s"${ d.keyword } ") }
		.reduceLeft { _ || _ }.withinParenthesis
	private lazy val declarationStartRegex = declarationModifierRegex.anyTimes + declarationKeywordRegex
	private val namedDeclarationStartRegex = {
		val operatorRegex = Regex.anyOf("!+-=/*&%?:<>|").oneOrMoreTimes.withinParenthesis
		val nameRegex = (((Regex.escape('_') + Regex.letterOrDigit).withinParenthesis || Regex.letter).withinParenthesis +
			(Regex.wordCharacter.anyTimes + Regex.letterOrDigit).withinParenthesis.noneOrOnce +
			(Regex.escape('_') + Regex.escape('=')).withinParenthesis.noneOrOnce).withinParenthesis
		
		declarationStartRegex + (operatorRegex || nameRegex).withinParenthesis
	}
	
	private lazy val extendsRegex = Regex(" extends ")
	private lazy val withRegex = Regex(" with ")
	
	private lazy val commaRegex = Regex.escape(',')
	private lazy val commaOutsideParenthesesRegex = commaRegex.ignoringParentheses
	private lazy val commaOutsideQuotationsRegex = commaRegex.ignoringQuotations
	
	private lazy val scalaDocStartRegex = Regex("\\/\\*\\*")
	private lazy val commentStartRegex = Regex("\\/\\*")
	private lazy val commentEndRegex = Regex("\\*\\/")
	private lazy val emptyScalaDocLineRegex = (Regex.newLine || Regex.whiteSpace || Regex.escape('\t') ||
		Regex.escape('*')).withinParenthesis.anyTimes
	private lazy val segmentSeparatorRegex = Regex.upperCaseLetter.oneOrMoreTimes +
		(Regex.escape('\t') || Regex.whiteSpace).withinParenthesis.oneOrMoreTimes +
		Regex.escape('-').oneOrMoreTimes
	
	def apply(path: Path) = {
		IterateLines.fromPath(path) { linesIter =>
			val iter = linesIter.pollable /*linesIter.map { line =>
				println("Reading: " + line)
				line
			}.pollable*/
			
			// Searches for package declaration first
			val filePackageString = iter.pollToNextWhere { _.nonEmpty }.filter(packageRegex.apply) match {
				case Some(packageLine) =>
					iter.skipPolled()
					packageLine.afterFirst("package ")
				case None => ""
			}
			// println(s"Read package: $filePackageString")
			
			// Next looks for import statements
			val importStatements = iter.collectWhile { line => line.isEmpty || importRegex(line) }
				.filter { _.nonEmpty }
				.map { _.afterFirst("import ") }
				.flatMap { importString =>
					// println(s"Read import: $importString")
					if (importString.contains('{')) {
						val (basePart, endPart) = importString.splitAtFirst("{").toTuple
						val trimmedBase = basePart.trim
						val endItems = endPart.untilFirst("}").split(',').toVector.map { _.trim }
						/*println(s"Which is split into ${endItems.size} parts after $trimmedBase (${
							endItems.mkString(", ")})")*/
						endItems.map { trimmedBase + _ }
					}
					else
						Vector(importString.trim)
				}
			val separatedImportStatements = importStatements.map { importStatement =>
				val (beginning, end) = importStatement.splitAtLast(".").toTuple
				end.notEmpty match {
					case Some(end) =>
						if (end == "_") {
							val (packagePart, targetPrefix) = beginning.splitAtLast(".").toTuple
							if (targetPrefix.isEmpty)
								filePackageString -> importStatement
							else
								packagePart -> s"$targetPrefix._"
						}
						else
							beginning -> end
					case None => filePackageString -> importStatement
				}
			}
			val packagePerString = separatedImportStatements.map { case (packageString, _) => packageString }
				.toSet.map { s: String => s -> Package(s) }.toMap
			val referencesPerTarget = separatedImportStatements
				.map { case (packageString, target) => target -> Reference(packagePerString(packageString), target) }
				.toMap
			// println(s"Read ${packagePerString.size} packages and ${referencesPerTarget.size} references")
			
			// Finally, finds and processes the object and/or class statements
			// Implicit references are included in the resulting file directly
			val builder = new FileBuilder(path.fileNameWithoutExtension, Package(filePackageString),
				referencesPerTarget.valuesIterator.filter { _.target.contains('_') }.toSet)
			// println(s"Poll before mapping: ${iter.poll}")
			val codeLineIterator = iter.map { line =>
				val indentation = line.takeWhile { _ == '\t' }.length
				CodeLine(indentation, line.drop(indentation))
			}.pollable
			// println(s"Poll after mapping: ${codeLineIterator.poll}")
			// println("Reading the instance data...")
			readAllItemsFrom(codeLineIterator, referencesPerTarget, builder)
			// Returns the parsed file
			builder.result()
		}
	}
	
	private def readAllItemsFrom(linesIter: PollingIterator[CodeLine], refMap: Map[String, Reference],
	                             parentBuilder: InstanceBuilderLike) =
	{
		var continue = true
		while (linesIter.hasNext && continue) {
			// println("Continuing to read the next item...")
			continue = readNextItemFrom(linesIter, refMap, parentBuilder)
		}
	}
	
	// Returns whether there may be more
	private def readNextItemFrom(linesIter: PollingIterator[CodeLine], refMap: Map[String, Reference],
	                             parentBuilder: InstanceBuilderLike): Boolean =
	{
		// Reads lines until there is a scaladoc start or a declaration start
		val startingLines = linesIter.collectTo { line =>
			scalaDocStartRegex.existsIn(line.code) ||
				namedDeclarationStartRegex.existsIn(line.code) ||
				annotationStartRegex.existsIn(line.code)
		}.dropWhile { _.isEmpty }
		val beforeFirstLine = startingLines.dropRight(1)
		
		// Checks what the next non-empty line looks like
		startingLines.lastOption.exists { firstLine =>
			// println(s"Item starts from: $firstLine")
			// Processes the scaladoc block if there is one
			val (scalaDoc, afterScalaDocLine) = {
				if (scalaDocStartRegex.existsIn(firstLine.code)) {
					if (commentEndRegex.existsIn(firstLine.code)) {
						val scalaDoc = firstLine.code.afterFirst("/**").untilFirst("*/").trim
						scalaDocFromLines(Vector(scalaDoc)) -> linesIter.nextOption().getOrElse(CodeLine.empty)
					}
					else {
						val trimmedFirstLine = firstLine.code.afterFirst("/**")
						val rawLines = linesIter.collectTo { line => commentEndRegex.existsIn(line.code) }
						if (rawLines.isEmpty)
							scalaDocFromLines(Vector(trimmedFirstLine)) ->
								linesIter.nextOption().getOrElse(CodeLine.empty)
						else {
							val middleLines = rawLines.dropRight(1)
								.map { _.code.dropWhile { c => c == '\t' || c == '*' || c == ' ' } }
							val lastLine = rawLines.last.code.untilFirst("*/")
								.dropWhile { c => c == '\t' || c == ' ' || c == '*' }
							scalaDocFromLines(trimmedFirstLine +: middleLines :+ lastLine) ->
								linesIter.nextOption().getOrElse(CodeLine.empty)
						}
					}
				}
				else
					ScalaDoc.empty -> firstLine
			}/*
			if (scalaDoc.isEmpty)
			{
				println("No scaladoc read")
				println(s"Next line: $afterScalaDocLine")
			}
			else
			{
				println(s"Read scaladoc with ${scalaDoc.parts.size} parts")
				println(s"Next line: $afterScalaDocLine")
			}*/
			// Collects lines before the first declaration, if necessary
			val (beforeDeclarationLines, declarationLine) = {
				if (namedDeclarationStartRegex.existsIn(afterScalaDocLine.code)) {
					// println("First line was found to be a declaration")
					Vector() -> Some(afterScalaDocLine)
				}
				else
					// Skips leading and trailing empty lines
					(afterScalaDocLine +:
						linesIter.collectUntil { line => namedDeclarationStartRegex.existsIn(line.code) })
						.dropWhile { _.isEmpty }.dropRightWhile { _.isEmpty } -> linesIter.nextOption()
			}
			// if (beforeDeclarationLines.nonEmpty)
			// 	println(s"Read ${beforeDeclarationLines.size} lines that were not included in a declaration")
			declarationLine match {
				// Case: Declaration found => parses it
				case Some(declarationLine) =>
					// Looks for annotations
					val (annotations, commentAndCodeLines) = (beforeFirstLine ++ beforeDeclarationLines)
						.divideWith { line =>
							if (line.code.startsWith("@"))
								Left(annotationFrom(line, refMap))
							else
								Right(line)
						}
					// Processes the "free" code before the declaration
					// - comments will be attached to the parsed declaration
					val (beforeDeclarationComments, beforeDeclarationCode) = commentAndCodeLines.divideWith { line =>
						if (line.code.startsWith("//"))
							Left(line.code.drop(2).trim)
						else
							Right(line.copy(indentation = line.indentation - declarationLine.indentation))
					}
					val filteredComments = beforeDeclarationComments.filter { !segmentSeparatorRegex(_) }
					parentBuilder.addFreeCode(beforeDeclarationCode
						.dropWhile { _.isEmpty }.dropRightWhile { _.isEmpty })
					/*
					println(s"Read declaration line: ${declarationLine.code}")
					if (filteredComments.nonEmpty)
						println(s"Read ${filteredComments.size} comments before the declaration")
					if (beforeDeclarationCode.nonEmpty)
						println(s"Read ${beforeDeclarationLines.size} lines of code before the declaration started")
					*/
					// Identifies the declaration in question
					val declarationStartRange = namedDeclarationStartRegex.firstRangeFrom(declarationLine.code).get
					val declarationStart = declarationLine.code.slice(declarationStartRange)
					val afterDeclarationStart = declarationLine.code.substring(declarationStartRange.exclusiveEnd)
					val declarationName = {
						if (declarationStart.contains(" "))
							declarationStart.afterLast(" ")
						else
							declarationStart
					}
					val declarationType = DeclarationType.values
						.find { d => declarationStart.contains(s"${ d.keyword } ") }.get
					// Parses prefixes and visibility
					val prefixes = declarationType.availablePrefixes
						.filter { prefix => declarationStart.contains(s"${ prefix.keyword } ") }
					val visibility = {
						if (declarationStart.contains("private "))
							Private
						else if (declarationStart.contains("protected "))
							Protected
						else
							Public
					}
					/*
					println(s"Interpreted: $visibility ${prefixes.mkString(" ")} ${
						declarationType.keyword} $declarationName")
					*/
					// Parses generic types list, if needed
					val (genericTypes, afterGenericTypes) = {
						if (declarationType.acceptsGenericTypes && afterDeclarationStart.startsWith("[")) {
							// TODO: Current version expects the type list to fit within the same line.
							//  Create a better version if necessary
							val (typesPart, afterTypes) = readOneLineBrackets(afterDeclarationStart.drop(1))
							genericTypesFrom(typesPart, refMap)._1 -> afterTypes
						}
						else
							Vector() -> afterDeclarationStart
					}
					// Parses parameter lists, if needed
					val (parameters, afterParameterLists) = {
						if (declarationType.acceptsParameterList && afterGenericTypes.startsWith("(")) {
							val (rawLists, remaining) = readRawParameterLists(afterGenericTypes, linesIter)
							/*println(s"Reading parameter list from ${
								rawLists.map { list => s"'$list'" }.mkString(" & ")} (${rawLists.size} lists) - remaining: '$remaining'")*/
							val parameters = parametersFrom(rawLists, refMap, scalaDoc)
							Some(parameters) -> remaining
						}
						else
							None -> afterGenericTypes.trim.notEmpty
					}
					/*
					parameters.foreach { p => println(s"Read ${p.lists.size} parameter lists and ${
						p.implicits.size} implicit parameters - remaining: '${afterParameterLists.getOrElse("")}'") }
					 */
					// Handles functions and instances differently
					declarationType match {
						case declarationType: FunctionDeclarationType =>
							// println("Interpreted as a function declaration")
							// Checks whether explicit parameter type has been specified
							val (explicitType, declarationEnd) = explicitTypeFrom(
								afterParameterLists.getOrElse(""), refMap)
							
							// Expects an assignment operator before a body
							// - if no operator is given, assumes that the function is abstract
							// println(s"Processing declaration end $declarationEnd")
							val body = {
								if (declarationEnd.contains('=')) {
									val lineAfterAssignment = declarationEnd.afterFirst("=").trim
									val firstBodyLine = {
										if (lineAfterAssignment.isEmpty)
											linesIter.nextOption() match
											{
												case Some(line) => line
												case None => CodeLine.empty
											}
										else
											CodeLine(lineAfterAssignment)
									}
									// Case: Function body is wrapped in a block => reads the block contents
									if (firstBodyLine.code.startsWith("{"))
									{
										val (block, _) = readBlock(firstBodyLine.mapCode { _.drop(1) }, linesIter)
										block.toCodeWith(refMap)
									}
									// Case: Function opens without a block but the main part of the function
									// consists of a block (e.g. abc match { ... })
									// => Wraps the function body in a block
									else if (firstBodyLine.code.contains('{') &&
										!firstBodyLine.code.afterLast("{").contains('}'))
									{
										val (beforeBlockStart, afterBlockStart) = firstBodyLine.code
											.splitAtLast("{").toTuple
										val (mainBlock, remaining) = readBlock(
											CodeLine(declarationLine.indentation, afterBlockStart), linesIter)
										combineLineAndBlock(CodeLine(firstBodyLine.indentation, beforeBlockStart),
											mainBlock, remaining, refMap)
									}
									// Case: Function opens without a block but the main part of the function
									// consists of a block (e.g. abc match { ... })
									// => Wraps the function body in a block
									else if (linesIter.pollOption.exists { _.code.startsWith("{") })
									{
										val (block, remaining) = readBlock(linesIter.next().mapCode { _.tail },
											linesIter)
										combineLineAndBlock(firstBodyLine, block, remaining, refMap)
									}
									// Case: Function is a single line function (possibly on multiple lines)
									// => Squashes the lines to a single code line
									else
									{
										// Collects the lines based on indentation
										// (hoping that they are indented correctly)
										val moreBodyLines = linesIter
											.collectWhile { _.indentation > declarationLine.indentation }
											.dropRightWhile { _.isEmpty }
										val allCodeLine = CodeLine(s"${ firstBodyLine.code }${ moreBodyLines.map { _.code }.mkString }")
										Code(Vector(allCodeLine), refMap.keySet.filter { target =>
											allCodeLine.code.contains(target) }.map(refMap.apply))
									}
								}
								else
									Code.empty
							}
							// Adds the parsed function (property or method) to the parent instance
							// Case: Parsed item is a method
							if (declarationType == FunctionD && parameters.exists { _.containsExplicits })
								parentBuilder.addMethod(MethodDeclaration(visibility, declarationName, genericTypes,
									parameters.get, body, explicitType, annotations, scalaDoc.description,
									scalaDoc.returnDescription, filteredComments,
									isOverridden = prefixes.contains(Override),
									isImplicit = prefixes.contains(Implicit), isLowMergePriority = false))
							// Case: Parsed item is a property
							else {
								val propertyType = declarationType match {
									case ValueD => if (prefixes.contains(Lazy)) LazyValue else ImmutableValue
									case VariableD => Variable
									case _ => ComputedProperty
								}
								val implicitParameters = parameters match
								{
									case Some(parameters) => parameters.implicits
									case None => Vector()
								}
								parentBuilder.addProperty(PropertyDeclaration(propertyType, declarationName, body,
									visibility, explicitType, implicitParameters, annotations,
									scalaDoc.description.nonEmptyOrElse(scalaDoc.returnDescription),
									filteredComments, isOverridden = prefixes.contains(Override),
									isImplicit = prefixes.contains(Implicit)))
							}
							true
						case declarationType: InstanceDeclarationType =>
							// println("Interpreted as an instance declaration")
							// Looks for extends portion
							// The instance body is always expected to be wrapped in a block,
							// which is processed separately
							val (extensions, contentBlock) = extensionsAndBlockFrom(
								afterParameterLists.map { CodeLine(declarationLine.indentation, _) },
								linesIter, refMap)
							val builder = new InstanceBuilder(visibility, prefixes, declarationType, declarationName,
								genericTypes, parameters, extensions, scalaDoc, filteredComments, annotations)
							contentBlock.foreach { block =>
								// Reads all available items from the block
								val blockLinesIterator = block.lines.iterator.pollable
								readAllItemsFrom(blockLinesIterator, refMap, builder)
							}
							parentBuilder.addNested(builder.result(refMap))
							true
					}
				// Case: No declaration found => adds read code lines as free code
				case None =>
					parentBuilder.addFreeCode(beforeDeclarationLines)
					false
			}
		}
	}
	
	private def annotationFrom(line: CodeLine, refMap: Map[String, Reference]) = {
		val (mainPart, rawParamsPart) = line.code.splitAtFirst("(").toTuple
		val (namePart, typesPart) = mainPart.splitAtFirst("[").toTuple
		val name = namePart.drop(1)
		val types = if (typesPart.nonEmpty) scalaTypesFrom(typesPart, refMap)._1 else Vector()
		val params = {
			if (rawParamsPart.nonEmpty)
				rawParamsPart.untilLast(")").splitIterator(commaOutsideQuotationsRegex)
					.map(JsonBunny.valueOf).toVector
			else
				Vector()
		}
		Annotation(name, types, params)
	}
	
	private def combineLineAndBlock(firstLine: CodeLine, block: ReadCodeBlock, remaining: Option[String],
	                                refMap: Map[String, Reference]) =
	{
		val codeBuilder = new CodeBuilder()
		// Adds the initial part
		codeBuilder.setIndentation(firstLine.indentation)
		codeBuilder += firstLine.code
		// Includes references
		codeBuilder.addReferences(refMap.keySet.filter(firstLine.code.contains).map(refMap.apply))
		// Adds the main part as a block of code (starting from 0 indentation)
		codeBuilder.addBlock(block.toCodeWith(refMap))
		// May add the remaining part separately (with indentation 1)
		remaining.filter { _.nonEmpty }.foreach { remaining =>
			codeBuilder.indent()
			codeBuilder += remaining
			codeBuilder.addReferences(
				refMap.keySet.filter(remaining.contains).map(refMap.apply))
		}
		codeBuilder.result()
	}
	
	// Expects tabulator strikes, whitespaces etc. to be removed from line beginnings
	private def scalaDocFromLines(lines: Vector[String]) =
	{
		// Skips empty lines from the beginning and the end
		val targetLines = lines.dropWhile(emptyScalaDocLineRegex.apply).dropRightWhile(emptyScalaDocLineRegex.apply)
		if (targetLines.isEmpty)
			ScalaDoc.empty
		else {
			val builder = new MultiMapBuilder[Option[ScalaDocKeyword], String]
			var lastKeyword: Option[ScalaDocKeyword] = None
			targetLines.foreach { line =>
				val (keyword, content) = extractScalaDocKeyword(line)
				if (keyword.nonEmpty)
					lastKeyword = keyword
				builder += lastKeyword -> content.dropWhile { c => c == '\n' || c == '\r' }
			}
			ScalaDoc(builder.result()
				.map { case (keyword, lines) => ScalaDocPart(lines, keyword) }
				.toVector.sortBy { _.keyword }
			)
		}
	}
	
	// Expects leading tabulator strikes etc. to be removed at this point
	private def extractScalaDocKeyword(line: String) =
	{
		if (line.startsWith("@"))
		{
			val (keywordPart, afterKeyword) = line.splitAtFirst(" ").toTuple
			val contentPartPointer = new MutatingOnce(afterKeyword, afterKeyword.afterFirst(" "))
			
			ScalaDocKeyword.matching(keywordPart.drop(1),
				contentPartPointer.value.untilFirst(" ")) -> contentPartPointer.value
		}
		else
			None -> line
	}
	
	private def extensionsAndBlockFrom(openLine: Option[CodeLine], moreLinesIter: PollingIterator[CodeLine],
	                                   refMap: Map[String, Reference]) =
	{
		// Case: Instance block starts on the first line
		if (openLine.exists { _.code.contains('{') })
		{
			val (beforeBlock, afterBlockStart) = openLine.get.code.splitAtFirst("{").toTuple
			val (block, _) = readBlock(CodeLine(openLine.get.indentation, afterBlockStart), moreLinesIter)
			extensionsFrom(Vector(beforeBlock), refMap) -> Some(block)
		}
		// Case: Instance block may start on a later line (after extension lines) or there may not be a code block
		else
		{
			// Looks for the block start, but terminates on a named declaration, comment or scaladoc
			// Collects the in-between lines as extension lines
			val extensionLinesBuilder = new VectorBuilder[String]()
			openLine.foreach { extensionLinesBuilder += _.code }
			var afterBlockPart: Option[CodeLine] = None
			while (afterBlockPart.isEmpty && moreLinesIter.pollOption.exists { line =>
				!line.code.startsWith("//") && !commentStartRegex.existsIn(line.code) &&
					!namedDeclarationStartRegex.existsIn(line.code)
			})
			{
				// Case: Extension and/or block start line found
				val line = moreLinesIter.next()
				line.code.optionIndexOf("{") match
				{
					// Case: Block starts on this line
					case Some(blockStartIndex) =>
						if (blockStartIndex > 0)
							extensionLinesBuilder += line.code.take(blockStartIndex)
						afterBlockPart = Some(line.mapCode { _.drop(blockStartIndex + 1) })
					// Case: This line is only extensions, still
					case None => extensionLinesBuilder += line.code
				}
			}
			val extensions = extensionsFrom(extensionLinesBuilder.result(), refMap)
			afterBlockPart match
			{
				// Case: Block was found
				case Some(afterBlockStart) =>
					val (block, _) = readBlock(afterBlockStart, moreLinesIter)
					extensions -> Some(block)
				// Case: Instance didn't contain a code block
				case None => extensions -> None
			}
		}
	}
	
	private def extensionsFrom(lines: Vector[String], refMap: Map[String, Reference]): Vector[Extension] =
	{
		// println(s"Reading extensions from: ${lines.map { line => s"'$line'" }.mkString(" + ") }")
		// Finds the line that contains the extends -keyword
		lines.findIndexWhere { line => extendsRegex.existsIn(line) || line.startsWith("extends ") ||
			line.endsWith(" extends") } match
		{
			// Case: Extends -keyword found => Parses the extensions from the remaining line part + remaining lines
			case Some(firstLineIndex) =>
				val firstLineRemain = lines(firstLineIndex).afterFirst("extends")
				// Individual extensions are separated with a " with "
				val parts = withRegex.split((firstLineRemain +: lines.drop(firstLineIndex + 1)).mkString(" "))
					.map { _.trim }.toVector
				// println(s"Found following parts: ${ parts.mkString(" & ") }")
				parts.map { partString =>
					// Parses the parent type
					val (parentType, afterType) = scalaTypeFrom(partString, refMap)
					// println(s"Found $parentType from $partString - remaining: '$afterType'")
					// Checks whether a constructor list should be included
					if (afterType.startsWith("("))
					{
						val (constructorLists, _) = readOneLineRawParameterLists(afterType.drop(1))
						val constructorAssignments = constructorLists
							.map { commaOutsideParenthesesRegex.split(_).toVector
								.map { addReferencesToCode(_, refMap) } }
						/*println(s"Read constructor list: ${
							constructorAssignments.map { list => s"(${list.mkString(", ")})" }.mkString }")*/
						Extension(parentType, constructorAssignments)
					}
					else
						Extension(parentType)
				}
			// Case: No extends -keyword found => no extensions
			case None => Vector()
		}
	}
	
	private def addReferencesToCode(code: String, refMap: Map[String, Reference]) =
		CodePiece(code, refMap.keySet.filter(code.contains).map(refMap.apply))
	
	private def explicitTypeFrom(string: String, refMap: Map[String, Reference]) =
	{
		// println(s"Reading explicit type from '$string'")
		if (string.startsWith(":")) {
			val (dataType, remaining) = scalaTypeFrom(string.drop(1).trim, refMap)
			// println(s"Found explicit type '$dataType' - remaining: '$remaining'")
			Some(dataType) -> remaining
		}
		else {
			// println("No explicit type found")
			None -> string
		}
	}
	
	private def parametersFrom(parameterListStrings: Vector[String], refMap: Map[String, Reference],
	                           scalaDoc: ScalaDoc) =
	{
		// println(s"\nReading parameters from ${ parameterListStrings.size } lines: ")
		// parameterListStrings.foreach { line => println(s"\t- $line") }
		
		// Handles the implicit parameter list separately, which is expected to be the last list, if present
		val (standardLists, implicitList) = {
			if (parameterListStrings.lastOption.exists { _.startsWith("implicit ") })
				parameterListStrings.dropRight(1) -> Some(parameterListStrings.last.afterFirst("implicit "))
			else
				parameterListStrings -> None
		}
		val standardParameters = standardLists.map { parameterListFrom(_, refMap, scalaDoc) }
		val implicitParameters = implicitList match
		{
			case Some(listString) => parameterListFrom(listString, refMap, scalaDoc)
			case None => Vector()
		}
		Parameters(standardParameters, implicitParameters)
	}
	
	private def parameterListFrom(parameterListString: String, refMap: Map[String, Reference], scalaDoc: ScalaDoc) =
	{
		// println(s"\nReading parameter list from: $parameterListString")
		if (parameterListString.isEmpty) {
			// println("\t=> Empty list")
			Vector()
		}
		else
		{
			val paramsBuilder = new VectorBuilder[Parameter]()
			val (firstParam, firstRemaining) = nextParameterFrom(parameterListString, refMap, scalaDoc)
			// println(s"First parameter is: $firstParam")
			paramsBuilder += firstParam
			var remaining = firstRemaining
			while (remaining.startsWith(",")) {
				// println(s"Parses next param starting from: $remaining")
				val (nextParam, nextRemaining) = nextParameterFrom(remaining.drop(1), refMap, scalaDoc)
				paramsBuilder += nextParam
				// println(s"Parsed parameter: $nextParam")
				remaining = nextRemaining
			}
			// println(s"Remaining part, which was not included in any parameter: $remaining")
			paramsBuilder.result()
		}
	}
	
	private def nextParameterFrom(string: String, refMap: Map[String, Reference], scalaDoc: ScalaDoc) =
	{
		// println(s"Parsing next parameter from: '$string'")
		
		// Parses name and data type
		val (namePart, remaining) = string.splitAtFirst(":").toTuple
		val trimmedNamePart = namePart.trim
		val (dataType, afterType) = scalaTypeFrom(remaining.trim, refMap)
		val (beforeName, name) = if (trimmedNamePart.contains(' ')) trimmedNamePart.splitAtLast(" ").toTuple else
			"" -> trimmedNamePart
		// Parses parameter prefix, if there is one
		val prefix = DeclarationType.values.find { t => beforeName.contains(t.keyword) }.map { declarationType =>
			val visibility = {
				if (beforeName.contains("private ")) Private
				else if (beforeName.contains("protected ")) Protected
				else Public
			}
			val prefixes = DeclarationPrefix.values.filter { p => beforeName.contains(p.keyword) }
			DeclarationStart(declarationType, visibility, prefixes)
		}
		// Reads parameter description from the scaladoc
		val description = scalaDoc.param(name)
		/*
		prefix.foreach { prefix => println(s"Parsed prefix: $prefix") }
		println(s"Parsed $name (from $trimmedNamePart)")
		println(s"Parsed type $dataType")
		*/
		// Case: Parameter has a default value
		if (afterType.startsWith("="))
			commaOutsideParenthesesRegex.startIndexIteratorIn(afterType).nextOption() match
			{
				// Case: There remains yet another parameter
				case Some(nextCommaIndex) =>
					/*
					println(s"Found default value: ${afterType.slice(1, nextCommaIndex).trim}")
					println("Continuing to the next parameter after the comma")
					 */
					Parameter(name, dataType, afterType.slice(1, nextCommaIndex).trim, prefix, description) ->
						afterType.drop(nextCommaIndex)
				// Case: This was the last parameter
				case None =>
					// println(s"Read the rest (${afterType.drop(1).trim}) as the default value")
					Parameter(name, dataType, afterType.drop(1).trim, prefix, description) -> ""
			}
		// Case: No default value provided
		else
		{
			// println(s"No parameter default value found. Leaves following code: '$afterType'")
			Parameter(name, dataType, prefix = prefix, description = description) -> afterType
		}
	}
	
	// Returns parsed type + remaining string
	private def scalaTypeFrom(string: String, refMap: Map[String, Reference]): (ScalaType, String) = {
		// println(s"\nParsing scala type from: '$string'")
		
		// Case: Starts with parentheses => Leads to either tuple or function type
		if (string.startsWith("(")) {
			//println("Tuple type")
			
			val (parenthesisPart, remainingPart) = readOneLineParentheses(string.drop(1))
			// Parses types within the parentheses
			val (parenthesesTypes, _) = scalaTypesFrom(parenthesisPart, refMap)
			
			// Case: Function type => Parses result type also
			if (remainingPart.startsWith("=>")) {
				val functionResultPart = remainingPart.afterFirst("=>").trim
				val (resultType, remaining) = scalaTypeFrom(functionResultPart, refMap)
				resultType.fromParameters(parenthesesTypes) -> remaining
			}
			// Case: Tuple type => wraps the types into a "basic" scala type
			// TODO: References should be handled properly
			else
				ScalaType.basic(parenthesesTypes.map { _.toScala }
					.reduceLeftOption { _.append(_, ", ") }.getOrElse(CodePiece.empty)
					.withinParenthesis.text) -> remainingPart
		}
		else {
			// Checks whether this is a call-by-name type
			val (mainPart, isCallByName) = if (string.startsWith("=>")) string.afterFirst("=>").trim -> true else
				string -> false
			// println(s"Type main part is '$mainPart'. Call by name: $isCallByName")
			// Parses the type name until generic types list or some interrupting character
			val mainPartEndIndex = mainPart.indexWhere { c => !c.isLetterOrDigit && c != '_' && c != '.' }
			val (mainTypeString, remaining) = if (mainPartEndIndex < 0) mainPart -> "" else
				mainPart.take(mainPartEndIndex) -> mainPart.drop(mainPartEndIndex).trim
			// println(s"Main type is: $mainTypeString - Remaining: $remaining")
			// Checks whether the main type is referencing a type
			val mainType = refMap.get(mainTypeString) match {
				case Some(ref) => Right(ref)
				case None => Left(mainTypeString)
			}
			// Parses generic types if available
			if (remaining.startsWith("[")) {
				// println("Reading generic type parameter list...")
				val (typesPart, afterTypes) = readOneLineBrackets(remaining.drop(1))
				// println(s"Types part is '$typesPart' - remaining: $afterTypes")
				val (types, _) = scalaTypesFrom(typesPart, refMap)
				// Checks for functional type
				if (afterTypes.startsWith("=>")) {
					val functionResultPart = afterTypes.afterFirst("=>").trim
					val (resultType, afterResult) = scalaTypeFrom(functionResultPart, refMap)
					resultType.fromParameters(Vector(ScalaType(mainType)(types))) -> afterResult
				}
				else
					ScalaType(mainType, types, if (isCallByName) CallByName else Standard) -> afterTypes
			}
			// Checks for functional type
			else if (remaining.startsWith("=>")) {
				val functionResultPart = remaining.afterFirst("=>").trim
				val (resultType, afterResult) = scalaTypeFrom(functionResultPart, refMap)
				resultType.fromParameters(Vector(ScalaType(mainType))) -> afterResult
			}
			else
				ScalaType(mainType, category = if (isCallByName) CallByName else Standard) -> remaining.trim
		}
	}
	
	private def scalaTypesFrom(string: String, refMap: Map[String, Reference]): (Vector[ScalaType], String) =
		typeListFrom(string) { scalaTypeFrom(_, refMap) }
	
	// Returns parsed type + remaining string
	private def genericTypeFrom(string: String, refMap: Map[String, Reference]): (GenericType, String) =
	{
		// Checks type parameter variance
		val (mainPart, variance) = TypeVariance.explicitValues.findMap { variance =>
			if (string.startsWith(variance.typePrefix))
				Some(string.drop(variance.typePrefix.length) -> variance)
			else
				None
		}.getOrElse(string -> Invariance)
		
		// Parses the type name until some interrupting character
		val mainPartEndIndex = mainPart.indexWhere { c => !c.isLetterOrDigit && c != '_' && c != '.' }
		val (typeName, remaining) = if (mainPartEndIndex < 0) mainPart -> "" else
			mainPart.take(mainPartEndIndex) -> mainPart.drop(mainPartEndIndex).trim
		
		// Checks whether a type limit is applied
		val (typeRequirement, afterType) = InheritanceLimitType.values
			.find { limitType => remaining.startsWith(limitType.toString) } match
		{
			case Some(limitType) =>
				val (limitingType, afterType) = scalaTypeFrom(remaining.drop(limitType.toString.length).trim, refMap)
				Some(TypeRequirement(limitingType, limitType)) -> afterType
			case None => None -> remaining
		}
		
		GenericType(typeName, typeRequirement, variance) -> afterType
	}
	
	private def genericTypesFrom(string: String, refMap: Map[String, Reference]) = {
		// println(s"Parsing a generic type from '$string'")
		typeListFrom(string) { genericTypeFrom(_, refMap) }
	}
	
	private def typeListFrom[A](string: String)(readType: String => (A, String)): (Vector[A], String) =
	{
		// Parses types as long as there are some remaining
		// Expects them to be separated by a comma
		val (firstType, firstRemain) = readType(string)
		val typesBuilder = new VectorBuilder[A]()
		typesBuilder += firstType
		
		var remaining = firstRemain
		while (remaining.startsWith(","))
		{
			val (nextType, nextRemain) = readType(remaining.drop(1).trim)
			typesBuilder += nextType
			remaining = nextRemain
		}
		typesBuilder.result() -> remaining
	}
	
	// Returns lists as lists of lines
	private def readRawParameterLists(listStartLine: String,
	                                  remainingLinesIter: PollingIterator[CodeLine]): (Vector[String], Option[String]) =
	{
		// println(s"Reading parameter list contents starting from line: '$listStartLine'")
		val listsBuilder = new VectorBuilder[String]()
		// Code that appears after the initial parameter lists will be stored here
		var codeAfter: Option[String] = None
		// Checks whether the first line starts a parameter list
		if (listStartLine.startsWith("(")) {
			// Wraps the starting lines as code lines without indentation
			// - doesn't utilize indentation in this method
			val (listLines, remainingAfter) = readBlockLike(CodeLine(listStartLine.drop(1)), remainingLinesIter,
				'(', ')')
			/*
			println(s"Immediately started parsing. Found: '${listLines.map { _.code }.mkString}' (${
				listLines.size} lines) - remaining: $remainingAfter")
			 */
			listsBuilder += listLines.map { _.code }.mkString
			// Collects lists while the remaining part keeps initiating new lists
			codeAfter = remainingAfter
			while (codeAfter.exists { _.startsWith("(") }) {
				val (listLines, remainingAfter) = readBlockLike(CodeLine(codeAfter.get.drop(1)), remainingLinesIter,
					'(', ')')
				/*
				println(s"Got ${ listLines.size } lines from ${codeAfter.get}:")
				listLines.foreach { l => println(s"- '$l'") }
				 */
				listsBuilder += listLines.map { _.code }.mkString
				codeAfter = remainingAfter
			}
		}
		// May continue to look for lists from the next lines
		if (codeAfter.nonEmpty)
			listsBuilder.result() -> codeAfter
		else
			remainingLinesIter.pollOption match {
				case Some(nextLine) =>
					val trimmedNextLine = nextLine.code.trim
					// Case: Next line opens a new parameter list => processes that and the potential lists after
					if (trimmedNextLine.startsWith("(")) {
						remainingLinesIter.skipPolled()
						// println("Uses recursion to go deeper")
						val (nextLists, remaining) = readRawParameterLists(trimmedNextLine, remainingLinesIter)
						// println(s"Found ${ nextLists.size } lines from the opened line: '$trimmedNextLine'")
						(listsBuilder.result() ++ nextLists) -> remaining
					}
					// Case: Next line doesn't open a new list => leaves it as it is
					else
						listsBuilder.result() -> None
				// Case: There are no more lines left
				case None => listsBuilder.result() -> None
			}
	}
	
	// Block start line must have the initiating { -char removed
	private def readBlock(blockStartLine: CodeLine, remainingLinesIter: Iterator[CodeLine]) =
	{
		val (blockLines, after) = readBlockLike(blockStartLine, remainingLinesIter, '{', '}')
		// Removes the unnecessary indentation and possible leading and trailing empty lines
		val actualLines = blockLines.dropWhile { _.isEmpty }.dropRightWhile { _.isEmpty }
		if (actualLines.isEmpty)
			ReadCodeBlock.empty -> after
		else
		{
			val zeroIndentLevel = actualLines.head.indentation
			ReadCodeBlock(actualLines.map { line => line.copy(indentation = line.indentation - zeroIndentLevel) }) ->
				after
		}
	}
	
	// Block start line must have the initiating opening character removed
	private def readBlockLike(blockStartLine: CodeLine, remainingLinesIter: Iterator[CodeLine],
	                          openChar: Char, closeChar: Char) =
	{
		// println(s"\nLooking for $openChar$closeChar blocks...")
		// Collects the block lines into a vector
		val blockLinesBuilder = new VectorBuilder[CodeLine]()
		// Keeps track of open blocks
		var openBlocksCount = 1
		var line = blockStartLine
		var remaining: Option[String] = None // Some when this process should return
		while (remaining.isEmpty) {
			// println(s"Next line: '$line'; Blocks currently open: $openBlocksCount")
			// Checks the current line
			openBlockCountAfter(line.code, openBlocksCount, openChar, closeChar) match {
				// Case: Block completes on this line => Splits it into two and returns
				case Right((blockPart, remainingLine)) =>
					// println(s"Block closes on this line after '$blockPart'; Remaining part is '$remainingLine'")
					blockLinesBuilder += line.withCode(blockPart.dropRight(1))
					remaining = Some(remainingLine.dropWhile { _ == ' ' })
				// Case: Block continues after this line => Moves to the next line, if possible
				case Left(newOpenBlocksCount) =>
					blockLinesBuilder += line
					openBlocksCount = newOpenBlocksCount
					if (remainingLinesIter.hasNext)
						line = remainingLinesIter.next()
					else
						remaining = Some("")
			}
		}
		blockLinesBuilder.result() -> remaining.filter { _.nonEmpty }
	}
	
	private def openBlockCountAfter(line: String, startCount: Int, openChar: Char, closeChar: Char) = {
		// Checks one character at a time, until openings are matching the closings or the line ends
		val indexIter = line.indices.iterator
		var openCount = startCount
		while (openCount > 0 && indexIter.hasNext) {
			val next = line(indexIter.next())
			// Case: Going deeper
			if (next == openChar)
				openCount += 1
			// Case: Closing the last opening
			else if (next == closeChar)
				openCount -= 1
		}
		// Case: Matching openings and closing found => Returns the block part and the remaining part
		if (openCount <= 0)
			indexIter.nextOption() match {
				case Some(remainingStartIndex) => Right(line.take(remainingStartIndex) -> line.drop(remainingStartIndex))
				case None => Right(line -> "")
			}
		// Case: Line ended while the block is still open => Returns the remaining depth
		else
			Left(openCount)
	}
	
	private def readOneLineRawParameterLists(line: String) =
	{
		val resultsBuilder = new VectorBuilder[String]()
		val (firstList, firstRemaining) = readOneLineParentheses(line)
		resultsBuilder += firstList
		var remaining = firstRemaining
		while (remaining.startsWith("("))
		{
			val (nextList, nextRemaining) = readOneLineParentheses(remaining.drop(1))
			resultsBuilder += nextList
			remaining = nextRemaining
		}
		resultsBuilder.result() -> remaining
	}
	
	private def readOneLineBrackets(line: String) =
		readOneLineBlockLike(line, '[', ']')
	
	private def readOneLineParentheses(line: String) =
		readOneLineBlockLike(line, '(', ')')
	
	// Returns block part and remaining part
	private def readOneLineBlockLike(line: String, openChar: Char, closeChar: Char) =
	{
		// println(s"\nReading a block of $openChar --- $closeChar from $line")
		
		val openOrCloseCharRegex = Regex.escape(openChar) || Regex.escape(closeChar)
		val changeIterator = openOrCloseCharRegex.startIndexIteratorIn(line)
		var currentDepth = 1
		var lastIndex = line.length
		while (currentDepth > 0 && changeIterator.hasNext) {
			lastIndex = changeIterator.next()
			// println(s"Encountered ${line(lastIndex)} at index $lastIndex. Current depth = $currentDepth")
			if (line(lastIndex) == openChar)
				currentDepth += 1
			else
				currentDepth -= 1
		}
		// println(s"The block part was interpreted to be: '${line.take(lastIndex)}'")
		line.take(lastIndex).trim -> line.drop(lastIndex + 1).trim
	}
}
