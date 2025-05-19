package utopia.vault.nosql.targeting

/**
  * Common trait for access points that can be filtered and/or extended
  * @author Mikko Hilpinen
  * @since 18.05.2025, v1.21
  */
trait Targeting[+A, +Val] extends TargetingLike[A, Val, Targeting[A, Val]]
