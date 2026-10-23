/*
 * Program 3.7 - Perfect number
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int isPerfect(int n) {
    int sum = 0;
    for (int i = 1; i <= n / 2; i++)
        if (n % i == 0) sum += i;
    return n > 1 && sum == n;
}

int main(void) {
    printf("Perfect numbers up to 1000: ");
    for (int i = 1; i <= 1000; i++)
        if (isPerfect(i)) printf("%d ", i);
    printf("\n");
    return 0;
}
