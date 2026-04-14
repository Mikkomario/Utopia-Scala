package utopia.flow.collection.mutable.iterator

object InsertBeforeIterator
{
	/**
	 * Creates a new iterator that inserts n item to a location defined by a find function
	 * @param coll Collection to which the items are inserted
	 * @param insert Collection from which additional items are inserted
	 * @param f A function which yields true for the item *before* which the 'insert' items should be yielded
	 * @tparam A Type of the iterated items
	 * @return A new iterator
	 */
	def apply[A, B >: A](coll: IterableOnce[A], insert: IterableOnce[B])(f: A => Boolean) =
		new InsertBeforeIterator[A, B](coll.iterator, insert.iterator)(f)
}

/**
 * An iterator that inserts n items before the first match of a find function.
 * Note: If said function always yields false, the items are inserted at the end of the source collection.
 * @author Mikko Hilpinen
 * @since 12.04.2026, v2.8.1
 */
class InsertBeforeIterator[O, I >: O](source: Iterator[O], insertSource: Iterator[I])(f: O => Boolean)
	extends Iterator[I]
{
	// ATTRIBUTES   -----------------------
	
	/**
	 * Caches the value that triggered the insert
	 */
	private var prepared: Option[O] = None
	/**
	 * Set to true once the insert has completed
	 */
	private var inserted = false
	
	
	// IMPLEMENTED  -----------------------
	
	override def hasNext: Boolean = {
		// Case: Inserting => The following item is ready => Always has next
		if (prepared.isDefined)
			true
		// Case: Not inserting => Has next if there are more source items, or if there's more to insert
		else
			source.hasNext || (!inserted && insertSource.hasNext)
	}
	
	override def next(): I = {
		// Case: Insert already performed => Delegates to source
		if (inserted)
			source.next()
		else
			prepared match {
				// Case: Currently inserting => Takes items from the insert iterator as long as possible
				case Some(afterInsert) =>
					insertSource.nextOption().getOrElse {
						// Case: End of insert => Yields the prepared item
						prepared = None
						inserted = true
						afterInsert
					}
				// Case: Not yet inserted => Checks whether to insert next
				case None =>
					source.nextOption() match {
						case Some(next) =>
							// Case: Insert triggered => Yields the first inserted item, if possible
							if (f(next))
								insertSource.nextOption() match {
									// Case: Inserts available
									//       => Prepares the triggering item to be yielded after the insert
									case Some(nextInsert) =>
										prepared = Some(next)
										nextInsert
									// Case: No inserts were available
									//       => Yields the triggering item and exits insert mode
									case None =>
										inserted = true
										next
								}
							// Case: No insert yet => Yields the next item
							else
								next
								
						// Case: Out of source => Inserts the next item
						case None => insertSource.next()
					}
			}
	}
}
