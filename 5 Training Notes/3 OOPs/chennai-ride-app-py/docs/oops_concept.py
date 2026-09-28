"""
OOP Concepts in Python — explained through a Rapido-style ride app (Chennai)
===========================================================================
Run:  python oops_concepts_python.py

A menu lets you run one concept at a time (or all of them), so you can
teach each concept separately. Standard library only.
"""

from abc import ABC, abstractmethod
from decimal import Decimal, ROUND_HALF_UP
import math


def header(title):
    print("\n" + "=" * 60)
    print(title)
    print("=" * 60)


# ---------------------------------------------------------------------------
# 1. CLASS AND OBJECT
#    A class is the blueprint; an object is a real thing built from it.
# ---------------------------------------------------------------------------
class Customer:
    def __init__(self, name, phone):      # constructor: runs when the object is created
        self.name = name                  # 'self' = this particular object
        self.phone = phone

    def greet(self):                      # instance method
        return f"Vanakkam {self.name}! Where do you want to go today?"


def demo_class_and_object():
    header("1. CLASS AND OBJECT")
    priya = Customer("Priya", "+91 98400 12345")    # object 1
    karthik = Customer("Karthik", "+91 98410 67890")  # object 2, same blueprint
    print(priya.greet())
    print(karthik.greet())
    print("Different objects?", priya is not karthik)


# ---------------------------------------------------------------------------
# 2. INSTANCE ATTRIBUTES vs CLASS ATTRIBUTES
#    Class attribute  -> ONE copy, shared by every object
#    Instance attribute -> each object gets its OWN copy
# ---------------------------------------------------------------------------
class RideCounter:
    GST_RATE = Decimal("0.05")   # class attribute: same GST for every ride
    total_rides = 0              # class attribute: shared counter

    def __init__(self, pickup, drop):
        RideCounter.total_rides += 1
        self.ride_id = f"RIDE-{RideCounter.total_rides:03d}"   # instance attribute
        self.pickup = pickup
        self.drop = drop


def demo_attributes():
    header("2. INSTANCE vs CLASS ATTRIBUTES")
    r1 = RideCounter("Egmore", "Tambaram")
    r2 = RideCounter("Velachery", "Guindy")
    print(r1.ride_id, r1.pickup, "->", r1.drop)
    print(r2.ride_id, r2.pickup, "->", r2.drop)
    print("Shared GST rate:", RideCounter.GST_RATE, "| Total rides:", RideCounter.total_rides)


# ---------------------------------------------------------------------------
# 3. ENCAPSULATION
#    Hide the data, expose only safe methods.
#    _name  -> "protected" by convention (please don't touch from outside)
#    __name -> "private" via name mangling (outside access fails)
#    @property -> read access without allowing direct changes
# ---------------------------------------------------------------------------
class Ride:
    def __init__(self, pickup, drop, otp):
        self.pickup = pickup
        self.drop = drop
        self._status = "REQUESTED"     # protected
        self.__otp = otp               # private: only Ride can see it
        self.__wrong_attempts = 0

    @property
    def status(self):                  # read-only: there is NO setter
        return self._status

    def start(self, entered_otp):      # the ONLY way to start a ride
        if self._status != "CAPTAIN_ARRIVED":
            raise ValueError(f"Cannot start a ride that is {self._status}")
        if entered_otp != self.__otp:
            self.__wrong_attempts += 1
            if self.__wrong_attempts == 3:
                self._status = "CANCELLED"
                raise ValueError("3 wrong OTPs - ride auto-cancelled")
            raise ValueError(f"Wrong OTP (attempt {self.__wrong_attempts}/3)")
        self._status = "IN_PROGRESS"

    def mark_arrived(self):
        if self._status != "REQUESTED":
            raise ValueError(f"Cannot arrive when ride is {self._status}")
        self._status = "CAPTAIN_ARRIVED"


class Wallet:
    def __init__(self, balance):
        self.__balance = Decimal(balance)

    @property
    def balance(self):
        return self.__balance

    def top_up(self, amount):          # validation lives INSIDE the class
        amount = Decimal(amount)
        if amount <= 0 or amount > 10000:
            raise ValueError("Top-up must be between ₹1 and ₹10,000")
        self.__balance += amount

    def pay(self, amount):
        amount = Decimal(amount)
        if amount > self.__balance:    # never a partial deduction
            raise ValueError(f"Insufficient balance: need ₹{amount}, have ₹{self.__balance}")
        self.__balance -= amount


def demo_encapsulation():
    header("3. ENCAPSULATION")
    ride = Ride("Egmore", "Tambaram", otp="4821")
    print("Status:", ride.status)

    try:
        ride.status = "COMPLETED"      # no setter -> blocked
    except AttributeError:
        print("✘ Cannot set status directly - must use ride methods")

    try:
        print(ride.__otp)              # name mangling -> blocked
    except AttributeError:
        print("✘ ride.__otp is not accessible from outside")

    try:
        ride.start("4821")             # wrong state -> blocked
    except ValueError as e:
        print("✘", e)

    ride.mark_arrived()
    try:
        ride.start("1111")
    except ValueError as e:
        print("✘", e)
    ride.start("4821")
    print("✔ Correct OTP. Status:", ride.status)

    wallet = Wallet("150")
    try:
        wallet.pay("200")
    except ValueError as e:
        print("✘", e)
    print("Balance unchanged:", wallet.balance)
    wallet.top_up("100")
    wallet.pay("200")
    print("✔ Paid ₹200. Balance now:", wallet.balance)


# ---------------------------------------------------------------------------
# 4. ABSTRACTION
#    Show WHAT a thing does, hide HOW. Abstract classes cannot be created
#    directly; child classes must fill in the abstract methods.
# ---------------------------------------------------------------------------
class Vehicle(ABC):
    def __init__(self, model, plate):
        self.model = model
        self.plate = plate

    @abstractmethod
    def base_fare(self): ...

    @abstractmethod
    def per_km_rate(self): ...

    @abstractmethod
    def seats(self): ...

    def calculate_fare(self, km):      # concrete: written ONCE, works for every vehicle
        fare = self.base_fare() + self.per_km_rate() * Decimal(str(km))
        return fare.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

    def __str__(self):
        return f"{type(self).__name__}: {self.model} ({self.plate})"


class Bike(Vehicle):
    def base_fare(self): return Decimal("15")
    def per_km_rate(self): return Decimal("6")
    def seats(self): return 1


class Auto(Vehicle):
    def base_fare(self): return Decimal("25")
    def per_km_rate(self): return Decimal("11")
    def seats(self): return 3


def demo_abstraction():
    header("4. ABSTRACTION")
    try:
        Vehicle("Generic", "TN-00-XX-0000")
    except TypeError as e:
        print("✘ Cannot create an abstract Vehicle:", str(e).split(" with")[0])
    bike = Bike("Honda Activa", "TN-09-AB-4521")
    print("✔", bike, "| 8 km fare: ₹", bike.calculate_fare(8))


# ---------------------------------------------------------------------------
# 5. INHERITANCE
#    single       : Bike(Vehicle)
#    multilevel   : Vehicle -> Cab -> CabPremium
#    hierarchical : User -> Customer, Captain
#    multiple     : a class with two parents (Python allows it, Java doesn't)
# ---------------------------------------------------------------------------
class Cab(Vehicle):                    # middle level: still abstract
    def seats(self): return 4

    def has_ac(self):                  # cab-only behaviour shared by all cabs
        return True


class CabEconomy(Cab):
    def base_fare(self): return Decimal("50")
    def per_km_rate(self): return Decimal("14")


class CabPremium(Cab):
    def base_fare(self): return Decimal("80")
    def per_km_rate(self): return Decimal("20")
    def seats(self): return 6          # overrides Cab's 4


class User(ABC):
    def __init__(self, user_id, name, phone):
        self.user_id = user_id
        self.name = name
        self.phone = phone

    @abstractmethod
    def role(self): ...


class RideCustomer(User):
    def __init__(self, user_id, name, phone, wallet_balance):
        super().__init__(user_id, name, phone)    # reuse parent's constructor
        self.wallet = Wallet(wallet_balance)

    def role(self): return "Customer"


class Captain(User):
    def __init__(self, user_id, name, phone, vehicle):
        super().__init__(user_id, name, phone)
        self.vehicle = vehicle
        self.location = "Egmore"

    def role(self): return "Captain"


class GPSTrackable:                    # used for multiple inheritance
    def track(self):
        return f"{self.name} is at {self.location}"


class TrackableCaptain(Captain, GPSTrackable):   # two parents
    pass


def demo_inheritance():
    header("5. INHERITANCE")
    premium = CabPremium("Toyota Innova Crysta", "TN-07-BX-1190")
    print("Multilevel:", " -> ".join(c.__name__ for c in CabPremium.__mro__[:-2]))
    print("CabPremium is a Cab?", isinstance(premium, Cab), "| is a Vehicle?", isinstance(premium, Vehicle))
    print("Inherited from Cab -> has_ac():", premium.has_ac(), "| Overridden seats():", premium.seats())

    selvi = RideCustomer(1, "Selvi", "+91 98401 11111", "300")
    murugan = Captain(2, "Murugan", "+91 98402 22222", Auto("Bajaj RE", "TN-22-CK-7813"))
    print("Hierarchical:", selvi.name, "->", selvi.role(), "|", murugan.name, "->", murugan.role())

    ravi = TrackableCaptain(3, "Ravi", "+91 98403 33333", Bike("TVS Jupiter", "TN-10-DE-3344"))
    print("Multiple:", ravi.track())
    print("MRO:", [c.__name__ for c in TrackableCaptain.__mro__])


# ---------------------------------------------------------------------------
# 6. POLYMORPHISM
#    Same call, different behaviour depending on the object.
# ---------------------------------------------------------------------------
class Payable(ABC):                    # acts like a Java interface
    @abstractmethod
    def pay(self, amount): ...


class UpiPayment(Payable):
    def __init__(self, upi_id): self.upi_id = upi_id
    def pay(self, amount): return f"₹{amount} paid via UPI ({self.upi_id})"


class CashPayment(Payable):
    def pay(self, amount): return f"₹{amount} collected in cash by captain"


class WalletPayment(Payable):
    def __init__(self, wallet): self.wallet = wallet
    def pay(self, amount):
        self.wallet.pay(amount)
        return f"₹{amount} paid from wallet (left: ₹{self.wallet.balance})"


class SmsNotifier:                     # duck typing: NO common parent at all
    def notify(self, msg): print("   [SMS]  ", msg)


class PushNotifier:
    def notify(self, msg): print("   [PUSH] ", msg)


class Money:                           # operator overloading
    def __init__(self, amount):
        self.amount = Decimal(str(amount)).quantize(Decimal("0.01"))

    def __add__(self, other): return Money(self.amount + other.amount)
    def __mul__(self, factor): return Money(self.amount * Decimal(str(factor)))
    def __lt__(self, other): return self.amount < other.amount
    def __str__(self): return f"₹{self.amount}"


def book_ride(pickup, drop, vehicle="Bike", payment=None):
    """Python has NO method overloading - a second def book_ride() would
    replace this one. Default arguments give the same flexibility."""
    return f"{vehicle} {pickup} -> {drop}, payment: {payment or 'choose later'}"


def demo_polymorphism():
    header("6. POLYMORPHISM")
    print("a) Method overriding - one calculate_fare(), different rates:")
    fleet = [Bike("Activa", "TN-01"), Auto("RE", "TN-02"),
             CabEconomy("Dzire", "TN-03"), CabPremium("Innova", "TN-04")]
    for v in fleet:                     # v is always 'a Vehicle'
        print(f"   {type(v).__name__:<11} 10 km = ₹{v.calculate_fare(10)}")

    print("b) Same pay() call, three behaviours:")
    methods = [UpiPayment("priya@okaxis"), CashPayment(), WalletPayment(Wallet("500"))]
    for m in methods:
        print("  ", m.pay(Decimal("120")))

    print("c) Duck typing - no shared parent, same method name:")
    for n in [SmsNotifier(), PushNotifier()]:
        n.notify("Captain Murugan is 3 mins away. OTP: 4821")

    print("d) Operator overloading on Money:")
    fare, gst = Money(180), Money(9)
    night = (fare + gst) * 1.2
    print(f"   {fare} + {gst} = {fare + gst} | with night charge: {night} | fare < night? {fare < night}")

    print("e) No overloading in Python - use default arguments:")
    print("  ", book_ride("Adyar", "Guindy"))
    print("  ", book_ride("Airport", "Anna Nagar", vehicle="CabPremium", payment="UPI"))


# ---------------------------------------------------------------------------
# 7. COMPOSITION vs AGGREGATION
#    Composition : the part is created and owned by the whole (dies with it)
#    Aggregation : the whole only refers to parts that live independently
# ---------------------------------------------------------------------------
class ComposedCaptain:
    def __init__(self, name, model, plate):
        self.name = name
        self.vehicle = Bike(model, plate)      # created INSIDE -> composition


class Trip:
    def __init__(self, customer, captain):
        self.customer = customer               # passed IN -> aggregation
        self.captain = captain


def demo_composition_aggregation():
    header("7. COMPOSITION vs AGGREGATION")
    cap = ComposedCaptain("Senthil", "TVS Jupiter", "TN-11-GH-5566")
    print("Composition: captain owns ->", cap.vehicle)

    divya = Customer("Divya", "+91 98404 44444")
    trip = Trip(divya, cap)
    del trip                                   # the trip ends...
    print("Aggregation: trip deleted, but customer still exists ->", divya.name)


# ---------------------------------------------------------------------------
# 8. CLASS METHODS and STATIC METHODS
#    @classmethod  -> works on the class; great for alternative constructors
#    @staticmethod -> a helper that needs neither object nor class
# ---------------------------------------------------------------------------
class Location:
    MATCH_RADIUS_KM = 3                        # class attribute shared by all locations
    PLACES = {
        "Egmore": (13.0732, 80.2609),
        "Tambaram": (12.9249, 80.1000),
        "Perungalathur": (12.9046, 80.0961),
        "Chromepet": (12.9516, 80.1462),
    }

    def __init__(self, name, lat, lng):
        self.name, self.lat, self.lng = name, lat, lng

    @classmethod
    def from_place(cls, name):                 # alternative constructor
        lat, lng = cls.PLACES[name]
        return cls(name, lat, lng)

    @staticmethod
    def haversine_km(a, b):                    # pure helper, no self/cls
        r = 6371
        dlat, dlng = math.radians(b.lat - a.lat), math.radians(b.lng - a.lng)
        h = (math.sin(dlat / 2) ** 2 + math.cos(math.radians(a.lat))
             * math.cos(math.radians(b.lat)) * math.sin(dlng / 2) ** 2)
        return round(2 * r * math.asin(math.sqrt(h)), 1)


def demo_class_static_methods():
    header("8. CLASS METHODS and STATIC METHODS")
    egmore = Location.from_place("Egmore")
    tambaram = Location.from_place("Tambaram")
    print(f"Egmore -> Tambaram ride: {Location.haversine_km(egmore, tambaram)} km")
    print(f"Captain is now at Tambaram. Match radius = {Location.MATCH_RADIUS_KM} km")
    for name in ["Egmore", "Chromepet", "Perungalathur"]:
        request = Location.from_place(name)
        d = Location.haversine_km(tambaram, request)
        ok = d <= Location.MATCH_RADIUS_KM
        print(f"   Request from {name:<13} {d:>5} km -> {'✔ can be matched' if ok else '✘ outside radius'}")


# ---------------------------------------------------------------------------
# 9. DUNDER (MAGIC) METHODS
#    __str__  -> friendly text for users (print)
#    __repr__ -> precise text for developers (debugging)
#    __eq__ / __hash__ -> equality, and use in sets / as dict keys
# ---------------------------------------------------------------------------
class Place:
    def __init__(self, name, lat, lng):
        self.__name, self.__lat, self.__lng = name, lat, lng

    def __str__(self): return self.__name
    def __repr__(self): return f"Place({self.__name!r}, {self.__lat}, {self.__lng})"

    def __eq__(self, other):
        return isinstance(other, Place) and (self.__lat, self.__lng) == (other.__lat, other.__lng)

    def __hash__(self): return hash((self.__lat, self.__lng))


def demo_dunder_methods():
    header("9. DUNDER (MAGIC) METHODS")
    a = Place("Egmore", 13.0732, 80.2609)
    b = Place("Egmore", 13.0732, 80.2609)
    print("str :", str(a))
    print("repr:", repr(a))
    print("a == b ?", a == b, "(same value)  |  a is b ?", a is b, "(different objects)")
    print("Unique pickup points in a set:", len({a, b}))


MENU = [
    ("Class and object", demo_class_and_object),
    ("Instance vs class attributes", demo_attributes),
    ("Encapsulation", demo_encapsulation),
    ("Abstraction", demo_abstraction),
    ("Inheritance", demo_inheritance),
    ("Polymorphism", demo_polymorphism),
    ("Composition vs aggregation", demo_composition_aggregation),
    ("Class methods and static methods", demo_class_static_methods),
    ("Dunder (magic) methods", demo_dunder_methods),
]


def show_menu():
    print("\n" + "=" * 60)
    print("   OOP CONCEPTS IN PYTHON  -  Chennai Ride App")
    print("=" * 60)
    for number, (title, _) in enumerate(MENU, start=1):
        print(f"  {number:>2}. {title}")
    print("   A. Run all concepts")
    print("   0. Exit")


def read_choice():
    """Returns the user's choice, or None when input ends (EOF / Ctrl+C)."""
    try:
        return input("\nEnter your choice: ").strip().lower()
    except (EOFError, KeyboardInterrupt):
        return None


def pause():
    try:
        input("\nPress Enter to return to the menu...")
    except (EOFError, KeyboardInterrupt):
        pass


def main():
    while True:
        show_menu()
        choice = read_choice()
        if choice is None or choice == "0":
            print("\nThank you! Keep practising OOP.")
            break
        if choice == "a":
            for _, demo in MENU:
                demo()
            pause()
        elif choice.isdigit() and 1 <= int(choice) <= len(MENU):
            MENU[int(choice) - 1][1]()          # call the chosen demo function
            pause()
        else:
            print(f"✘ Invalid choice '{choice}'. Enter 1-{len(MENU)}, A or 0.")


if __name__ == "__main__":
    import sys
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")   # ₹ and ✔ on Windows too
    main()