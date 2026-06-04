export const isValidEvent = (event) => {
  if (!event) return false;

  const validName =
    typeof event.eventName === "string" && event.eventName.trim() !== "";

  const validPrice =
    typeof event.ticketPrice === "number" && event.ticketPrice > 0;

  const validAvailable =
    typeof event.ticketsAvailable === "number" && event.ticketsAvailable >= 0;

  const validSold =
    typeof event.ticketsSold === "number" &&
    event.ticketsSold >= 0 &&
    event.ticketsSold <= event.ticketsAvailable;

  const validStatus = ["Active", "Sold Out", "Cancelled"].includes(
    event.status,
  );

  return validName && validPrice && validAvailable && validSold && validStatus;
};
