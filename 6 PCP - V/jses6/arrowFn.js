// arrow.js
// Anonymous Function, Arrow Function, map, filter, reduce

// ----------------------------
// Anonymous Function (Function Expression)
// ----------------------------

const sum = function (a, b) {
  return a + b;
};

console.log(typeof sum); // function
sum(10, 9);

// ----------------------------
// Arrow Function - Implicit Return
// ----------------------------

const square = (s) => s * s; // single expression → implicit return

// ----------------------------
// Arrow Function - Explicit Return
// ----------------------------

const quad = (a, b, c) => {
  const dis = b * b - 4 * a * c;

  if (dis < 0) return "No Roots";
  else if (dis == 0) return "one root";
  else return "Two Roots";
};

// ----------------------------
// Returning Object from Arrow Function
// ----------------------------

const employee = (person) => ({ ...person, job: "Trainer" });
// Wrap object inside ( ) to return object directly

employee({ name: "DK", age: 34 });

// ----------------------------
// Traditional Loop (Product)
// ----------------------------

const numbers = [1, 2, 3, 4, 5];

let prod = 1;
for (let i = 0; i < numbers.length; i++) {
  prod *= numbers[i];
}

// ----------------------------
// map() - Transforms Array
// ----------------------------

const doubled = numbers.map((value, index) => value * 2);

// ----------------------------
// filter() - Filters Elements
// ----------------------------

const evenNumber = numbers.filter((value, index) => value % 2 == 0);

// ----------------------------
// reduce() - Reduces to Single Value
// ----------------------------

const product = numbers.reduce((acc, iter) => acc * iter, 1);
