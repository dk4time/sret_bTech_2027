export function solveKnapsack(data) {
  const { availableHours, vehicles } = data;

  const n = vehicles.length;

  const dp = Array.from({ length: n + 1 }, () =>
    Array(availableHours + 1).fill(0),
  );

  for (let i = 1; i <= n; i++) {
    const { maintenanceHours, priority } = vehicles[i - 1];

    for (let hours = 0; hours <= availableHours; hours++) {
      if (maintenanceHours <= hours) {
        dp[i][hours] = Math.max(
          priority + dp[i - 1][hours - maintenanceHours],

          dp[i - 1][hours],
        );
      } else {
        dp[i][hours] = dp[i - 1][hours];
      }
    }
  }

  const selectedVehicles = [];

  let remainingHours = availableHours;

  let totalHours = 0;

  for (let i = n; i > 0; i--) {
    if (dp[i][remainingHours] !== dp[i - 1][remainingHours]) {
      const vehicle = vehicles[i - 1];

      selectedVehicles.push(vehicle);

      totalHours += vehicle.maintenanceHours;

      remainingHours -= vehicle.maintenanceHours;
    }
  }

  selectedVehicles.reverse();

  return {
    totalPriority: dp[n][availableHours],

    totalHours,

    selectedVehicles,
  };
}
