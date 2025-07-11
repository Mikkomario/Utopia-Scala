package utopia.logos.database.access.text.word

import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides word -based filtering for other types of access points.
  * @param wrapped Wrapped access point. Expected to include word.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class FilterByWord[+A <: FilterableView[A]](wrapped: A) 
	extends FilterWords[A] with FilterableViewWrapper[A]

