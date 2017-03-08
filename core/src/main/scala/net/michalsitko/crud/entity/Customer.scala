package net.michalsitko.crud.entity

// this small ADT hierachy just for lenses showcase
case class Customer(firstName: String, lastName: String, address: Address)

case class Address(no: String, street: String, city: String,
  state: String, zip: String)
