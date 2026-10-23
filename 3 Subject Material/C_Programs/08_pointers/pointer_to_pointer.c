/*
 * Program 8.2 - Pointer to pointer
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int x = 5;
    int *p = &x;
    int **pp = &p;

    **pp = 25;
    printf("x = %d, *p = %d, **pp = %d\n", x, *p, **pp);
    return 0;
}
