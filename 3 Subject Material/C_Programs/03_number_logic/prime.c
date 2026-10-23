/*
 * Program 3.3 - Prime check and primes in a range
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int isPrime(int n) {
    if (n < 2) return 0;
    for (int i = 2; i * i <= n; i++)
        if (n % i == 0) return 0;
    return 1;
}

int main(void) {
    printf("29 : %s\n", isPrime(29) ? "prime" : "not prime");
    printf("91 : %s\n", isPrime(91) ? "prime" : "not prime");
    printf("1  : %s\n", isPrime(1)  ? "prime" : "not prime");
    printf("Primes up to 50: ");
    for (int i = 1; i <= 50; i++)
        if (isPrime(i)) printf("%d ", i);
    printf("\n");
    return 0;
}
