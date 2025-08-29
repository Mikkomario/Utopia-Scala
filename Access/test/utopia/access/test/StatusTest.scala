package utopia.access.test

import utopia.access.model.enumeration.Status

/**
 * Tests Status enumeration set-up
 *
 * @author Mikko Hilpinen
 * @since 28.08.2025, v1.6
 */
object StatusTest extends App
{
	println(Status.values.mkString(", "))
	println(Status(204))
}
