// spread.js
// Demonstrates Spread and Rest Operator usage

// --------------------
// Object Spread
// --------------------

const Person = { name: "DK", age: 35 };

// Creates a shallow copy and adds new property
const employee = { ...Person, job: "Trainer" };

// Object destructuring
const { name, job } = employee;

console.log(employee.name); // DK

// --------------------
// Array Spread
// --------------------

const array1 = [1, 2, 3, 4];

// Creates new array by expanding array1
const array2 = [...array1, 5, 6];

console.log(array2); // [1, 2, 3, 4, 5, 6]

// --------------------
// Rest Operator in Function
// --------------------

function sum(...nums) {
  // Collects all arguments into an array

  const [a, b] = nums; // Array destructuring

  console.log(typeof nums, nums, nums[2]);
}

sum(1, 2);
sum(1, 2, 3);
sum(1, 2, 5, "", 9.9);
