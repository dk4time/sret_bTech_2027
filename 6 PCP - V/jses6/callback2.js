// -----------------------------------
// 4. Example 2 – Multiple Timers
// -----------------------------------

console.log("Start");

setTimeout(() => {
  console.log("3000");
}, 3000);

setTimeout(() => {
  console.log("2000");
}, 2000);

setTimeout(() => {
  console.log("5000");
}, 5000);

console.log("End");

// Output:
// Start
// End
// 2000
// 3000
// 5000
