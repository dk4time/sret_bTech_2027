/*
 * Program 3.2 - Armstrong number
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int countDigits(int n) {
    int c = 0;
    do { c++; n /= 10; } while (n > 0);
    return c;
}

int power(int base, int exp) {
    int r = 1;
    while (exp-- > 0) r *= base;
    return r;
}

int isArmstrong(int n) {
    int k = countDigits(n), sum = 0, t = n;
    while (t > 0) {
        sum += power(t % 10, k);
        t /= 10;
    }
    return sum == n;
}

int main(void) {
    printf("153  : %s\n", isArmstrong(153)  ? "Armstrong" : "No");
    printf("9474 : %s\n", isArmstrong(9474) ? "Armstrong" : "No");
    printf("123  : %s\n", isArmstrong(123)  ? "Armstrong" : "No");
    printf("Armstrong numbers 1-1000: ");
    for (int i = 1; i <= 1000; i++)
        if (isArmstrong(i)) printf("%d ", i);
    printf("\n");
    return 0;
}
