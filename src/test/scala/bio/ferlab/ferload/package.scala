package bio.ferlab

import cats.effect.IO
import cats.effect.unsafe.implicits.global

package object ferload {
  extension[T] (t: IO[T]) def unwrap: T = t.unsafeRunSync()
}
