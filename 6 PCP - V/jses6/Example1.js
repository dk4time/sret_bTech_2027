console.log("Start");

setTimeout(() => {
  console.log("Zero");
}, 0);

setTimeout(() => {
  console.log("2000");
}, 2000);

setTimeout(() => {
  console.log("5000");
}, 5000);

console.log("End");

//output: Start End Zero 2000 5000
