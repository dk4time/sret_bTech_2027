/*
 * Program 3.4 - GCD and LCM
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int gcd(int a, int b) {
    while (b != 0) {
        int r = a % b;
        a = b;
        b = r;
    }
    return a;
}

int main(void) {
    int a = 12, b = 18;
    int g = gcd(a, b);
    printf("GCD(%d, %d) = %d\n", a, b, g);
    printf("LCM(%d, %d) = %d\n", a, b, a / g * b);
    return 0;
}
