// -----------------------------------
// 3. Example 1 – Execution Order
// -----------------------------------

console.log("Start");

setTimeout(() => {
  console.log("SetTimeOut");
}, 3000);

console.log("End");

// Output:
// Start
// End
// SetTimeOut
