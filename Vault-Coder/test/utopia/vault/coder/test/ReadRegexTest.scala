package utopia.vault.coder.test

import utopia.flow.parse.string.Regex
import utopia.coder.model.scala.declaration.{DeclarationPrefix, DeclarationType}

/**
  * Testing regular expressions used in scala parsing
  * @author Mikko Hilpinen
  * @since 2.11.2021, v1.3
  */
object ReadRegexTest extends App
{
	private val visibilityRegex = (Regex("protected ") || Regex("private ")).withinParenthesis
	private val declarationPrefixRegex = DeclarationPrefix.values.map { p => Regex(s"${ p.keyword } ") }
		.reduceLeft { _ || _ }.withinParenthesis
	private val declarationModifierRegex = (visibilityRegex || declarationPrefixRegex).withinParenthesis
	private val declarationKeywordRegex = DeclarationType.values.map { d => Regex(s"${ d.keyword } ") }
		.reduceLeft { _ || _ }.withinParenthesis
	private val declarationStartRegex = declarationModifierRegex.anyTimes + declarationKeywordRegex
	private val namedDeclarationStartRegex = declarationStartRegex +
		((Regex.escape('_') + Regex.letterOrDigit).withinParenthesis || Regex.letter).withinParenthesis +
		(Regex.word + Regex.letterOrDigit).withinParenthesis.noneOrOnce +
		(Regex.escape('_') + Regex.escape('=')).withinParenthesis.noneOrOnce
	val testRegex = Regex("protected |private ")
	private lazy val segmentSeparatorRegex = (Regex.escape('/') * 2) + Regex.whiteSpace +
		Regex.upperCaseLetter.oneOrMoreTimes + Regex.escape('\t').oneOrMoreTimes +
		Regex.escape('-').oneOrMoreTimes
	
	val testData = "object DescriptionData extends FromModelFactoryWithSchema[DescriptionData]"
	assert(!visibilityRegex.existsIn(testData))
	assert(!declarationPrefixRegex.existsIn(testData))
	assert(!declarationModifierRegex.existsIn(testData))
	assert(declarationKeywordRegex.existsIn(testData))
	assert(declarationStartRegex.existsIn(testData))
	assert(Regex.word.existsIn(testData))
	Regex.word.matchesIteratorFrom(testData).foreach(println)
	assert(Regex.word.matchesIteratorFrom(testData).contains("DescriptionData"))
	
	assert(testRegex("protected "))
	assert(testRegex("private "))
	assert(!testRegex("p"))
	
	println(namedDeclarationStartRegex.string)
	assert(namedDeclarationStartRegex.existsIn(testData))
	assert(namedDeclarationStartRegex.existsIn("case class DescriptionData(roleId: Int, languageId: Int, text: String, authorId: Option[Int] = None, "))
	assert(segmentSeparatorRegex.apply("// ATTRIBUTES\t--------------"))
	assert(!segmentSeparatorRegex.apply("// Some tests"))
	
	assert(namedDeclarationStartRegex.findFirstFrom("def value_=(newValue: Value) = _value = newValue").get ==
		"def value_=")
	val test = ((Regex.escape('_') + Regex.letterOrDigit).withinParenthesis || Regex.letter).withinParenthesis
	println((Regex.escape('_') + Regex.letterOrDigit).withinParenthesis)
	println(namedDeclarationStartRegex.findFirstFrom("val _a = 3").get)
	println(test.findFirstFrom("_a = 3").get)
	println((declarationStartRegex + test).findFirstFrom("val _a = 3").get)
	println((Regex.escape('_') + Regex.letterOrDigit).findFirstFrom("val _a = 3").get)
	assert(namedDeclarationStartRegex.findFirstFrom("val _a = 3").get == "val _a")
	assert(namedDeclarationStartRegex.findFirstFrom("val _1 = 3").get == "val _1")
	assert(namedDeclarationStartRegex.findFirstFrom("val asd = 3").get == "val asd")
	
	println("Success!")
}
