package netconfig_extensions

import collection.JavaConversions
import com.google.common.collect.ImmutableList

object CollectionUtils {
    import JavaConversions._
    implicit def asImmutableList1[T](xs:Seq[T]):ImmutableList[T] = {
      ImmutableList.copyOf(JavaConversions.asIterable(xs))
    }
    implicit def asImmutableList2[T:Manifest](xs:Array[T]):ImmutableList[T] = {
      ImmutableList.copyOf(JavaConversions.asIterable(xs.toSeq))
    }
}