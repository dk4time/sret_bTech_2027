/*
 * Program 4.7 - Find the missing number from 1 to n
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int a[] = {1, 2, 4, 5, 6};          /* numbers 1..6, one missing */
    int n = 6, sum = 0;
    for (int i = 0; i < n - 1; i++) sum += a[i];
    printf("Missing number = %d\n", n * (n + 1) / 2 - sum);
    return 0;
}
