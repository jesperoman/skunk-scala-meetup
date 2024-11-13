package se.oeman.meetup

import io.circe.Codec as CirceCodec
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

final case class Todo(
  id: Option[Int],
  name: String,
  completed: Boolean
)

object Todo:
  val codec: Codec[Todo] = (int4.opt *: varchar *: bool).to[Todo]

  val insert: Query[String, Todo] =
    sql"""|INSERT INTO todos (name)
          |VALUES ($varchar)
          |RETURNING id, name, completed""".stripMargin.query(codec)

  val list: Query[Void, Todo] =
    sql"""|SELECT id, name, completed
          |FROM todos""".stripMargin.query(codec)

  val get: Query[Int, Todo] =
    sql"""|SELECT id, name, completed
          |FROM todos
          |WHERE id = $int4""".stripMargin.query(codec)

  val update: Command[(String, Boolean, Int)] =
    sql"""|UPDATE todos
          |SET
          |name = $varchar,
          |completed = $bool
          |WHERE id = $int4
          |""".stripMargin.command

  given CirceCodec[Todo] =
    CirceCodec
      .forProduct3("id", "name", "completed")(
        apply
      )(todo =>
        (
          todo.id,
          todo.name,
          todo.completed
        )
      )
