package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.{Changing, ListenableLazyWrapper}

object LazyTripleMergeMirror
{
	/**
	  * Creates a new mirror which lazily merges three sources.
	  * Uses a simpler mirror implementation if the sources involve static values.
	  * @param source1 First source pointer
	  * @param source2 Second source pointer
	  * @param source3 Third source pointer
	  * @param merge A function for merging the values of these sources
	  * @tparam O1 Type of the values in the first source
	  * @tparam O2 Type of the values in the second source
	  * @tparam O3 Type of the values in the third source
	  * @tparam R Type of merge results
	  * @return A lazily merging mirror
	  */
	def of[O1, O2, O3, R](source1: Changing[O1], source2: Changing[O2], source3: Changing[O3])
	                     (merge: (O1, O2, O3) => R) =
	{
		// Optimizes the listener-usage
		source1.fixedValue match {
			case Some(v1) =>
				source2.fixedValue match {
					case Some(v2) =>
						source3.fixedValue match {
							case Some(v3) => ListenableLazy { merge(v1, v2, v3) }
							case None => LazyMirror(source3) { merge(v1, v2, _) }
						}
					case None =>
						source3.fixedValue match {
							case Some(v3) => source2.lazyMap { merge(v1, _, v3) }
							case None => LazyMergeMirror(source2, source3) { merge(v1, _, _) }
						}
				}
			case None =>
				source2.fixedValue match {
					case Some(v2) =>
						source3.fixedValue match {
							case Some(v3) => LazyMirror(source1) { merge(_, v2, v3) }
							case None => LazyMergeMirror(source1, source3) { merge(_, v2, _) }
						}
					case None =>
						source3.fixedValue match {
							case Some(v3) => LazyMergeMirror(source1, source2) { merge(_, _, v3) }
							case None => apply(source1, source2, source3)(merge)
						}
				}
		}
	}
	
	/**
	  * Creates a new mirror which lazily merges three sources
	  * @param source1 First source pointer
	  * @param source2 Second source pointer
	  * @param source3 Third source pointer
	  * @param merge A function for merging the values of these sources
	  * @tparam O1 Type of the values in the first source
	  * @tparam O2 Type of the values in the second source
	  * @tparam O3 Type of the values in the third source
	  * @tparam R Type of merge results
	  * @return A lazily merging mirror
	  */
	def apply[O1, O2, O3, R](source1: Changing[O1], source2: Changing[O2], source3: Changing[O3])
	                        (merge: (O1, O2, O3) => R) =
		new LazyTripleMergeMirror(source1, source2, source3)(merge)
}

/**
  * A lazily calculating pointer that bases its value on three pointers and a merge function
  * @author Mikko Hilpinen
  * @since 16.8.2024, v2.4.1
  */
class LazyTripleMergeMirror[+O1, +O2, +O3, +Reflection](source1: Changing[O1], source2: Changing[O2],
                                                        source3: Changing[O3])
                                                       (merge: (O1, O2, O3) => Reflection)
	extends ListenableLazyWrapper[Reflection]
{
	// ATTRIBUTES	-------------------------------
	
	private val cache = ResettableLazy.listenable { merge(source1.value, source2.value, source3.value) }
	private lazy val listener = ChangeListener.onAnyChange { cache.reset(); Continue }
	
	
	// INITIAL CODE	-------------------------------
	
	source1.addHighPriorityListener(listener)
	source2.addHighPriorityListener(listener)
	source3.addHighPriorityListener(listener)
	
	
	// IMPLEMENTED	-------------------------------
	
	override protected def wrapped = cache
}
