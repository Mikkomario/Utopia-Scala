package utopia.reflection.test

import utopia.reflection.text.Regex

/**
  * This app tests basic regular expressions
  * @author Mikko Hilpinen
  * @since 2.5.2019, v1+
  */
object RegexTest extends App
{
	// 1) Digit
	val digit = Regex.digit
	println(digit)
	
	assert(digit("1"))
	assert(!digit("99"))
	assert(digit.filterNot("Mamma321") == "Mamma")
	assert(digit.filter("Mamma321") == "321")
	assert(digit.findFirstFrom("Mamma321").get == "3")
	
	// 2) Non-digit
	val nonDigit = Regex.nonDigit
	println(nonDigit)
	
	assert(!nonDigit("1"))
	assert(!nonDigit("99"))
	assert(!nonDigit("AA"))
	assert(nonDigit("a"))
	assert(nonDigit.filter("Mamma321") == "Mamma")
	assert(nonDigit.filterNot("Mamma321") == "321")
	assert(nonDigit.findFirstFrom("Mamma321").get == "M")
	
	// 3) Followed by
	val combo = nonDigit + digit
	println(combo)
	
	assert(combo("A3"))
	assert(!combo("3A"))
	assert(!combo("3"))
	assert(!combo("A"))
	assert(!combo("AA"))
	assert(combo.filterNot("Mamma321") == "Mamm21")
	assert(combo.filter("Mamma321") == "a3")
	assert(combo.findFirstFrom("Mamma321").get == "a3")
	
	// 4) Alpha
	val alpha = Regex.alpha
	println(alpha)
	
	assert(alpha("A"))
	assert(!alpha("1"))
	assert(!alpha("-"))
	assert(alpha.filterNot("Mamma321") == "321")
	assert(alpha.filter("Mamma321") == "Mamma")
	
	// 5) Alpha sequence
	val alphaSeq = alpha.oneOrMoreTimes
	println(alphaSeq)
	
	assert(alphaSeq("ABC"))
	assert(!alphaSeq("ABC1"))
	assert(alphaSeq("A"))
	assert(alphaSeq.filterNot("Mamma321") == "321")
	assert(alphaSeq.filter("Mamma321") == "Mamma")
	assert(alphaSeq.findFirstFrom("Mamma321").get == "Mamma")
	
	// 5) Numeric
	val numeric = Regex.numeric
	println(numeric)
	
	assert(numeric("1"))
	assert(numeric("321"))
	assert(numeric("-321"))
	assert(!numeric("--222"))
	assert(!numeric("2A"))
	assert(numeric.filterNot("-273.6 €") == ". €")
	assert(numeric.filter("-273.6 €") == "-2736")
	assert(numeric.findFirstFrom("-273.6 €").get == "-273")
	
	// 6) Alphanumeric
	val alphaNumeric = Regex.alphaNumeric
	println(alphaNumeric)
	
	assert(alphaNumeric("1"))
	assert(alphaNumeric("A"))
	assert(!alphaNumeric("1A"))
	assert(alphaNumeric.filter("Mamma321") == "Mamma321")
	assert(alphaNumeric.filterNot("Mamma321") == "")
	assert(alphaNumeric.findFirstFrom("Mamma321").get == "M")
	
	// 7) Decimal
	val decimal = Regex.decimal
	println(decimal)
	
	assert(decimal("1.2"))
	assert(decimal("-1.2"))
	assert(decimal("1"))
	assert(decimal("-1"))
	assert(decimal("11.223455"))
	assert(!decimal("3A"))
	assert(!decimal("A"))
	assert(!decimal("-"))
	assert(!decimal("."))
	assert(decimal.filterNot("-273.6 €") == " €")
	assert(decimal.filter("-273.6 €") == "-273.6")
	assert(decimal.findFirstFrom("-273.6 €").get == "-273.6")
	
	// 8) Decimal positive
	val decimalPositive = Regex.decimalPositive
	println(decimalPositive)
	
	assert(decimalPositive("1.2"))
	assert(!decimalPositive("-1.2"))
	assert(decimalPositive("1"))
	assert(!decimalPositive("-1"))
	assert(decimalPositive.filterNot("-273.6 €") == "- €")
	assert(decimalPositive.filter("-273.6 €") == "273.6")
	
	println("Success!")
}
