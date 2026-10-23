/*
 * Program 1.1 - Size of data types
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    printf("char    : %zu\n", sizeof(char));
    printf("int     : %zu\n", sizeof(int));
    printf("float   : %zu\n", sizeof(float));
    printf("double  : %zu\n", sizeof(double));
    printf("long    : %zu\n", sizeof(long));
    printf("pointer : %zu\n", sizeof(int *));
    return 0;
}
