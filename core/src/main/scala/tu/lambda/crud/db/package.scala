package tu.lambda.crud

import java.sql.Connection

import cats.data.Kleisli
import cats.effect.IO
import doobie.KleisliInterpreter
import doobie.free.connection.ConnectionIO

package object db {
  implicit class InterpretConnectionIO[T](program: ConnectionIO[T]) {
    def interpret: Kleisli[IO, Connection, T] =
      program.foldMap[Kleisli[IO, Connection, ?]](KleisliInterpreter[IO].ConnectionInterpreter)
  }
}
