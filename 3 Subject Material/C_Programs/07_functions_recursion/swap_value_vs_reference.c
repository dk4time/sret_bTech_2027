/*
 * Program 7.1 - Call by value vs call by reference
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

void swapValue(int a, int b) { int t = a; a = b; b = t; }
void swapRef(int *a, int *b) { int t = *a; *a = *b; *b = t; }

int main(void) {
    int x = 10, y = 20;
    swapValue(x, y);
    printf("After swapValue: x = %d, y = %d\n", x, y);
    swapRef(&x, &y);
    printf("After swapRef  : x = %d, y = %d\n", x, y);
    return 0;
}
