package net.michalsitko.crud.service

import net.michalsitko.crud.entity.Customer

class CustomerService {
  def changeCustomerStreet(customer: Customer, newStreet: String): Customer = {
    // without lenses:
    customer.copy(address = customer.address.copy(street = newStreet))
  }

  def changeCustomerStreetWithLenses(customer: Customer, newStreet: String): Customer = {
    import com.softwaremill.quicklens._

    customer.modify(_.address.street).setTo(newStreet)
  }
}
