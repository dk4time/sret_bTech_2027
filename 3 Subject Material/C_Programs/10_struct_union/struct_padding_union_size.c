/*
 * Program 10.2 - Structure padding and union size
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

struct A { char c; int i; char d; };
struct B { int i; char c; char d; };
union  U { int i; char c; double d; };

int main(void) {
    printf("sizeof(struct A) = %zu\n", sizeof(struct A));
    printf("sizeof(struct B) = %zu\n", sizeof(struct B));
    printf("sizeof(union U)  = %zu\n", sizeof(union U));
    return 0;
}
