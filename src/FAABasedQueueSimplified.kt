import java.util.concurrent.atomic.*
import kotlin.math.*

/**
 * @author Belousov Timofey
 */
class FAABasedQueueSimplified<E> : Queue<E> {
    private val infiniteArray = AtomicReferenceArray<Any?>(1024) // conceptually infinite array
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)

    override fun enqueue(element: E) {
        while(true){
            val curTail = enqIdx.getAndIncrement()
            if(infiniteArray.compareAndSet(curTail.toInt(), null, element)){
                return
            }
        }
    }

    private fun isEmpty(): Boolean {
        while(true) {
            val head = deqIdx.get()
            val tail = enqIdx.get()
            if(deqIdx.get() != head) continue

            return tail <= head
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            if (isEmpty()) return null

            val head = deqIdx.getAndIncrement()
            if(infiniteArray.compareAndSet(head.toInt(), null, POISONED)){
                continue
            } else {
                return infiniteArray.getAndSet(head.toInt(), POISONED) as E
            }
        }
    }

    override fun validate() {
        for (i in 0 until min(deqIdx.get().toInt(), enqIdx.get().toInt())) {
            check(infiniteArray[i] == null || infiniteArray[i] == POISONED) {
                "`infiniteArray[$i]` must be `null` or `POISONED` with `deqIdx = ${deqIdx.get()}` at the end of the execution"
            }
        }
        for (i in max(deqIdx.get().toInt(), enqIdx.get().toInt()) until infiniteArray.length()) {
            check(infiniteArray[i] == null || infiniteArray[i] == POISONED) {
                "`infiniteArray[$i]` must be `null` or `POISONED` with `enqIdx = ${enqIdx.get()}` at the end of the execution"
            }
        }
    }
}

private val POISONED = Any()
