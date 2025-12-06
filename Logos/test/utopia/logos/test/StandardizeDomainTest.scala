package utopia.logos.test

import utopia.logos.model.stored.url.Domain

/**
 * Tests Domain.standardize(...)
 * @author Mikko Hilpinen
 * @since 05.12.2025, v0.7
 */
object StandardizeDomainTest extends App
{
	assert(Domain.regex("www.pko.fi"))
	assert(Domain.regex("WWW.LAKKA.FI"))
	
	assert(Domain.standardize("http://pko.fi") == "http://www.pko.fi", Domain.standardize("http://pko.fi"))
	assert(Domain.standardize("WWW.LAKKA.FI") == "http://WWW.LAKKA.FI", Domain.standardize("WWW.LAKKA.FI"))
}
