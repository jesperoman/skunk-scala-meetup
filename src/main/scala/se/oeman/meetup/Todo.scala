package se.oeman.meetup

final case class Todo(
  id: Option[Int],
  name: String,
  completed: Boolean
)
