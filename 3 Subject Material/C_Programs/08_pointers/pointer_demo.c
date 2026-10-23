/*
 * Program 8.1 - Pointer basics and pointer arithmetic
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int a[5] = {10, 20, 30, 40, 50};
    int *p = a;

    printf("%d %d %d\n", *p, *(p + 2), p[4]);
    p++;
    printf("After p++: %d\n", *p);
    printf("Elements between a[4] and a[0]: %td\n", &a[4] - &a[0]);
    return 0;
}
