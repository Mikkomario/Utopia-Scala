package utopia.inception.test

import utopia.inception.handling.mutable.{Handleable, Killable}

class TestObject(val index: Int) extends Handleable with Killable