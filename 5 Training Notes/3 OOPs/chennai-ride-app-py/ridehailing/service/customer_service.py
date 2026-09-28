"""CustomerService — registration, wallet top-ups and moving around the city."""

from __future__ import annotations

from ridehailing.model.common.location import Location
from ridehailing.model.common.money import Money
from ridehailing.model.user.customer import Customer
from ridehailing.time.simulated_clock import SimulatedClock


class CustomerService:
    def __init__(self, clock: SimulatedClock) -> None:
        self._clock = clock
        self._customers: list[Customer] = []

    def register_customer(self, name: str, phone: str, home_place: str, wallet_balance: Money) -> Customer:
        customer = Customer(name, phone, Location.from_place_name(home_place), wallet_balance,
                            joined_at=self._clock.now)
        self._customers.append(customer)
        return customer

    def top_up(self, customer: Customer, amount: Money) -> Money:
        """Add money to the wallet (positive, max ₹10,000 per transaction). Returns the new balance."""
        customer.top_up(amount)
        return customer.wallet_balance

    def relocate(self, customer: Customer, place_name: str) -> None:
        """The customer travelled somewhere on their own. Refused if it would be teleporting."""
        customer.relocate_to(Location.from_place_name(place_name), self._clock.now)

    @property
    def customers(self) -> tuple[Customer, ...]:
        return tuple(self._customers)

    def find(self, name: str) -> Customer:
        for customer in self._customers:
            if customer.name == name:
                return customer
        raise KeyError(f"No customer called {name}")
