package utopia.inception.test

import utopia.inception.handling.HandlerType

case object TestHandlerType extends HandlerType
{
	override def supportedClass = classOf[TestObject]
}