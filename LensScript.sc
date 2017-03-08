import net.michalsitko.crud.entity.{Address, Customer}
import net.michalsitko.crud.service.CustomerService

val address = Address("23", "Zielona", "Rybnik", "Slaskie", "44-400")
val customer = Customer("Jan", "Kowalski", address)

val service = new CustomerService

val changedCustomer = service.changeCustomerStreet(customer, "Czerwona")

val changedCustomer2 = service.changeCustomerStreetWithLenses(customer, "Czerwona")