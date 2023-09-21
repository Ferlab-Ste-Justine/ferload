package bio.ferlab.ferload

import cats.effect.IO
import cats.effect.unsafe.implicits.global

package object endpoints {
  extension[T] (t: IO[T]) def unwrap: T = t.unsafeRunSync()
}
