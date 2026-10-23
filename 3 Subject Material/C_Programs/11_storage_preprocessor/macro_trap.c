/*
 * Program 11.2 - The macro trap
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

#define SQUARE_BAD(x)  x * x
#define SQUARE(x)      ((x) * (x))

int main(void) {
    printf("SQUARE_BAD(2 + 3) = %d\n", SQUARE_BAD(2 + 3));
    printf("SQUARE(2 + 3)     = %d\n", SQUARE(2 + 3));
    return 0;
}
