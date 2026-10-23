/*
 * Program 3.5 - Sum and count of digits
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int n = 50721, sum = 0, count = 0, t = n;
    while (t > 0) {
        sum += t % 10;
        count++;
        t /= 10;
    }
    printf("Number %d: digits = %d, sum = %d\n", n, count, sum);
    return 0;
}
