/*
 * Program 8.4 - Array decays to a pointer inside a function
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

void show(int *arr) {
    printf("Inside function: sizeof = %zu\n", sizeof(arr));
}

int main(void) {
    int a[10];
    printf("In main       : sizeof = %zu\n", sizeof(a));
    show(a);
    return 0;
}
