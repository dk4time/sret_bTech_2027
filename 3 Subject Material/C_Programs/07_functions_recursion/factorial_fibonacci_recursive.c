/*
 * Program 7.2 - Factorial and Fibonacci (recursive)
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

long fact(int n) { return (n <= 1) ? 1 : n * fact(n - 1); }
int  fib(int n)  { return (n <= 1) ? n : fib(n - 1) + fib(n - 2); }

int main(void) {
    printf("5! = %ld\n", fact(5));
    printf("fib(10) = %d\n", fib(10));
    return 0;
}
