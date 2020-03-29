package utopia.inception.test

import utopia.inception.util.Filter

class TestEventFilter(val requiredId: Int) extends Filter[TestEvent]
{
	override def apply(item: TestEvent) = item.index == requiredId
}