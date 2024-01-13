import java.util.concurrent.atomic.*

/**
 * @author Belousov Timofey
 */
class FAABasedQueue<E> : Queue<E> {
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)
    private val head: AtomicReference<Segment>
    private val tail: AtomicReference<Segment>

    init {
        val initial = Segment(0)
        head = AtomicReference(initial)
        tail = AtomicReference(initial)
    }

    private fun isEmpty(): Boolean {
        while(true) {
            val head = deqIdx.get()
            val tail = enqIdx.get()
            if(deqIdx.get() != head) continue

            return tail <= head
        }
    }

    private fun findSegment(start: Segment, id: Long): Segment{
        if(start.id > id) throw IllegalArgumentException("WTF?")

        var curSegment = start
        while(curSegment.id < id){
            val nextSegment = curSegment.next.get()
            if(nextSegment == null){
                val newSegment = Segment(curSegment.id + 1)
                curSegment.next.compareAndSet(null, newSegment)
                continue
            }
            curSegment = nextSegment
        }
        return curSegment
    }

    override fun enqueue(element: E) {
        while(true){
            var curTail = tail.get()
            val curIdx = enqIdx.getAndIncrement()

            val segment = findSegment(curTail, curIdx / SEGMENT_SIZE)

            while (segment.id > curTail.id) {
                if (tail.compareAndSet(curTail, segment)) {
                    break
                }
                curTail = tail.get()
            }

            if(segment.cells.compareAndSet((curIdx % SEGMENT_SIZE).toInt(), null, element)){
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            if (isEmpty()) return null

            var curHead = head.get()
            val curIdx = deqIdx.getAndIncrement()

            val segment = findSegment(curHead, curIdx / SEGMENT_SIZE)

            while (segment.id > curHead.id) {
                if (head.compareAndSet(curHead, segment)) {
                    break
                }
                curHead = head.get()
            }

            if(segment.cells.compareAndSet((curIdx % SEGMENT_SIZE).toInt(), null, POISONED)){
                continue
            } else {
                return segment.cells.getAndSet((curIdx % SEGMENT_SIZE).toInt(), POISONED) as E
            }
        }
    }
}

private class Segment(val id: Long) {
    val next = AtomicReference<Segment?>(null)
    val cells = AtomicReferenceArray<Any?>(SEGMENT_SIZE)
}

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2

private val POISONED = Any()
