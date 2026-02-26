// callback.js
// Demonstrates Callback & Asynchronous behavior

// -----------------------------------
// 1. Basic Callback using setTimeout
// -----------------------------------

setTimeout(() => {
  console.log("SetTimeOut");
}, 3000);

// -----------------------------------
// 2. setInterval + clearInterval
// -----------------------------------

const intervalId = setInterval(() => {
  console.log("SetInterval");
}, 2000);

// Stop interval after 10 seconds
setTimeout(() => {
  clearInterval(intervalId);
}, 10 * 1000);
