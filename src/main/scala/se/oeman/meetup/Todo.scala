package se.oeman.meetup

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

final case class Todo(
  id: Option[Int],
  name: String,
  completed: Boolean
)

object Todo:
  val insert: Query[String, Int] =
    sql"""|INSERT INTO todos (name)
          |VALUES ($varchar)
          |RETURNING id""".stripMargin.query(int4)
