/*
 * Program 7.3 - Power and sum of digits (recursive)
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

long power(int b, int e) { return (e == 0) ? 1 : b * power(b, e - 1); }
int digitSum(int n)      { return (n == 0) ? 0 : n % 10 + digitSum(n / 10); }

int main(void) {
    printf("2^10 = %ld\n", power(2, 10));
    printf("digitSum(9875) = %d\n", digitSum(9875));
    return 0;
}
